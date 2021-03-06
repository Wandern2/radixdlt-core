/*
 * (C) Copyright 2020 Radix DLT Ltd
 *
 * Radix DLT Ltd licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the
 * License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.  See the License for the specific
 * language governing permissions and limitations under the License.
 */

package org.radix.api.jsonrpc;

import com.google.common.io.CharStreams;
import com.radixdlt.atomos.RadixAddress;
import com.radixdlt.store.SearchCursor;
import com.radixdlt.store.StoreIndex;
import com.radixdlt.store.LedgerSearchMode;
import com.radixdlt.middleware2.store.EngineAtomIndices;
import com.radixdlt.engine.AtomStatus;
import com.radixdlt.common.AID;
import com.radixdlt.serialization.DsonOutput.Output;
import com.radixdlt.serialization.Serialization;
import com.radixdlt.store.LedgerEntryStore;
import io.undertow.server.HttpServerExchange;

import java.util.ArrayList;
import java.util.Objects;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.radix.api.services.AtomsService;
import org.radix.network2.addressbook.AddressBook;

import com.radixdlt.universe.Universe;
import org.radix.universe.system.LocalSystem;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Stateless Json Rpc 2.0 Server
 */
public final class RadixJsonRpcServer {
	private static final long DEFAULT_MAX_REQUEST_SIZE = 1024L * 1024L;

	/**
	 * Maximum request size in bytes
	 */
	private final long maxRequestSizeBytes;

	/**
	 * Atom schema to first validate against
	 */
	private final Schema atomSchema;

	/**
	 * Service to submit atoms through
	 */
	private final AtomsService atomsService;

	/**
	 * Store to query atoms from
	 */
    private final LedgerEntryStore ledger;

	private final Serialization serialization;
	private final LocalSystem localSystem;
	private final AddressBook addressBook;
	private final Universe universe;

	public RadixJsonRpcServer(Serialization serialization,
	                          LedgerEntryStore ledger,
	                          AtomsService atomsService,
	                          Schema atomSchema,
	                          LocalSystem localSystem,
	                          AddressBook addressBook,
	                          Universe universe) {
		this(serialization, ledger, atomsService, atomSchema, localSystem, addressBook, universe, DEFAULT_MAX_REQUEST_SIZE);
	}

	public RadixJsonRpcServer(Serialization serialization,
	                          LedgerEntryStore ledger,
	                          AtomsService atomsService,
	                          Schema atomSchema,
	                          LocalSystem localSystem,
	                          AddressBook addressBook,
	                          Universe universe,
	                          long maxRequestSizeBytes) {
		this.serialization = Objects.requireNonNull(serialization);
		this.ledger = Objects.requireNonNull(ledger);
		this.atomsService = Objects.requireNonNull(atomsService);
		this.atomSchema = Objects.requireNonNull(atomSchema);
		this.localSystem = Objects.requireNonNull(localSystem);
		this.addressBook = Objects.requireNonNull(addressBook);
		this.universe = Objects.requireNonNull(universe);
		this.maxRequestSizeBytes = maxRequestSizeBytes;
	}

    /**
     * Extract a JSON RPC API request from an HttpServerExchange, handle it as usual and return the response
     *
     * @param exchange The JSON RPC API request
     * @return The response
     */
    public String handleChecked(HttpServerExchange exchange) throws IOException {
	    exchange.setMaxEntitySize(maxRequestSizeBytes);
	    exchange.startBlocking();
        String requestBody = CharStreams.toString(new InputStreamReader(exchange.getInputStream(), StandardCharsets.UTF_8));

        return this.handleChecked(requestBody);
    }

	/**
	 * Handle the string JSON-RPC request with size checks, return appropriate error if oversized
	 * @param jsonRpcRequest The string JSON-RPC request
	 * @return The response to the request, could be a JSON-RPC error
	 */
	String handleChecked(String jsonRpcRequest) {
		// one char is 2 bytes
	    if (jsonRpcRequest.length() * 2 > maxRequestSizeBytes) {
		    return JsonRpcUtil.errorResponse(
		        JSONObject.NULL,
			    JsonRpcUtil.OVERSIZED_REQUEST,
			    "request too big: " + jsonRpcRequest.length() * 2 + " > " + maxRequestSizeBytes
		    ).toString();
	    }

	    return handle(new JSONObject(jsonRpcRequest)).toString();
	}

    /**
     * Handle a certain JSON RPC request and return the response
     *
     * @param jsonRpcRequest The JSON RPC API request
     * @return The response
     */
    private JSONObject handle(JSONObject jsonRpcRequest) {
        Object id = null;
        try {
        	if (!jsonRpcRequest.has("id")) {
        		return JsonRpcUtil.errorResponse(JSONObject.NULL, JsonRpcUtil.INVALID_REQUEST_CODE, "id missing");
			}

            id = jsonRpcRequest.get("id");
			final Object result;
			final String method = jsonRpcRequest.getString("method");
			final Object paramsObject = jsonRpcRequest.get("params");
			switch (method) {
				case "Ledger.getAtom":
					if (!(paramsObject instanceof JSONObject)) {
						return JsonRpcUtil.errorResponse(id, -32000, "params should be a JSONObject" , new JSONObject());
					} else {
						JSONObject params = (JSONObject) paramsObject;
						if (!params.has("aid")) {
							return JsonRpcUtil.errorResponse(id, -32000, "aid not present", new JSONObject());
						}

						String aidString = params.getString("aid");
						try {
							result = atomsService.getAtomsByAtomId(AID.from(aidString));
						} catch (RuntimeException e) {
							return JsonRpcUtil.errorResponse(id, -32000, "Atom not found", new JSONObject());
						}
					}

					break;
				case "Ledger.getAtoms":
					if (!(paramsObject instanceof JSONObject)) {
						return JsonRpcUtil.errorResponse(id, -32000, "No address present", new JSONObject());
					} else {
						JSONObject params = (JSONObject) paramsObject;
						if (!params.has("address")) {
							return JsonRpcUtil.errorResponse(id, -32000, "No address present", new JSONObject());
						}

						final String addressString = params.getString("address");
						final RadixAddress address = RadixAddress.from(addressString);

						StoreIndex index = new StoreIndex(EngineAtomIndices.IndexType.DESTINATION.getValue(), address.getUID().toByteArray());
						List<AID> collectedAids = new ArrayList<>();
						SearchCursor cursor = ledger.search(StoreIndex.LedgerIndexType.DUPLICATE, index, LedgerSearchMode.EXACT);
						while (cursor != null) {
							collectedAids.add(cursor.get());
							cursor = cursor.next();
						}
						result = collectedAids;
					}

					break;
				case "Universe.getUniverse":
					result = this.universe;
					break;
                case "Network.getLivePeers":
                    result = this.addressBook.recentPeers().collect(Collectors.toList());
                    break;
                case "Network.getPeers":
                    result = this.addressBook.peers().collect(Collectors.toList());
                    break;
                case "Network.getInfo":
                    result = localSystem;
                    break;
                case "Ping":
                    result = new JSONObject()
                        .put("response", "pong")
                        .put("timestamp", java.lang.System.currentTimeMillis());
                    break;
				case "Atoms.submitAtom":
					if (!(paramsObject instanceof JSONObject)) {
						return JsonRpcUtil.errorResponse(id, -32000, "No atom present", new JSONObject());
					} else {
						JSONObject jsonAtom = (JSONObject) paramsObject;

						try {
							atomSchema.validate(jsonAtom);
						} catch (ValidationException e) {
							return JsonRpcUtil.errorResponse(id, -32000, "Schema Error", e.toJSON());
						}

						final AID atomId = atomsService.submitAtom(jsonAtom, null);
						result = new JSONObject()
							.put("status", AtomStatus.PENDING_CM_VERIFICATION)
							.put("aid", atomId)
							.put("timestamp", System.currentTimeMillis());
					}
					break;
				case "Atoms.getAtomStatus":
					if (!(paramsObject instanceof JSONObject) || !((JSONObject) paramsObject).has("aid")) {
						return JsonRpcUtil.errorResponse(id, -32000, "No aid present", new JSONObject());
					} else {
						String aidString = ((JSONObject) paramsObject).getString("aid");
						final AID aid = AID.from(aidString);
						AtomStatus atomStatus;
						if (ledger.contains(aid)) {
							atomStatus = AtomStatus.STORED;
						} else {
							atomStatus = AtomStatus.DOES_NOT_EXIST;
						}
						result = new JSONObject().put("status", atomStatus.toString());
					}
					break;
                default:
                    return JsonRpcUtil.methodNotFoundResponse(id);
            }

            JSONObject response = new JSONObject();
			response.put("id", id);
			response.put("jsonrpc", "2.0");

			// FIXME: Bit of a hack for now
			if (result instanceof List) {
				List<?> list = (List<?>) result;
				JSONArray resultArray = new JSONArray();
				list.stream().map(o -> serialization.toJsonObject(o, Output.API)).forEach(resultArray::put);
				response.put("result", resultArray);
			} else {
				response.put("result", serialization.toJsonObject(result, Output.API));
			}

			return response;

		} catch (Exception e) {
			if (jsonRpcRequest.has("params") && jsonRpcRequest.get("params") instanceof JSONObject) {
				return JsonRpcUtil.errorResponse(id,-32000, e.getMessage(), jsonRpcRequest.getJSONObject("params"));
			} else {
				return JsonRpcUtil.errorResponse(id,-32000, e.getMessage());
			}
		}
	}
}
