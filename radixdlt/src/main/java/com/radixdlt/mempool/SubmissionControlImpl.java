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

package com.radixdlt.mempool;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import javax.inject.Inject;

import org.json.JSONObject;
import org.radix.atoms.events.AtomExceptionEvent;
import org.radix.events.Events;
import org.radix.logging.Logger;
import org.radix.logging.Logging;
import org.radix.validation.ConstraintMachineValidationException;

import com.radixdlt.common.AID;
import com.radixdlt.common.Atom;
import com.radixdlt.constraintmachine.CMError;
import com.radixdlt.engine.RadixEngine;
import com.radixdlt.serialization.Serialization;

class SubmissionControlImpl implements SubmissionControl {
	private static final Logger log = Logging.getLogger("submission");

	private final Mempool mempool;
	private final RadixEngine radixEngine;
	private final Serialization serialization;
	private final Events events;

	@Inject
	SubmissionControlImpl(Mempool mempool, RadixEngine radixEngine, Serialization serialization, Events events) {
		this.mempool = Objects.requireNonNull(mempool);
		this.radixEngine = Objects.requireNonNull(radixEngine);
		this.serialization = Objects.requireNonNull(serialization);
		this.events = Objects.requireNonNull(events);
	}

	@Override
	public void submitAtom(Atom atom) throws MempoolFullException, MempoolDuplicateException {
		Optional<CMError> validationError = this.radixEngine.staticCheck(atom);
		if (validationError.isPresent()) {
			CMError error = validationError.get();
			ConstraintMachineValidationException ex = new ConstraintMachineValidationException(atom, error.getErrMsg(), error.getDataPointer());
			log.info(
				String.format(
					"Rejecting atom %s with constraint machine error '%s' at '%s'.",
					atom.getAID(),
					error.getErrorDescription(),
					error.getDataPointer()
				)
			);
			this.events.broadcast(new AtomExceptionEvent(ex, atom.getAID()));
		} else {
			this.mempool.addAtom(atom);
		}
	}

	@Override
	public AID submitAtom(JSONObject atomJson, Consumer<Atom> deserialisationCallback)
		throws MempoolFullException, MempoolDuplicateException {
		Atom atom = this.serialization.fromJsonObject(atomJson, Atom.class);
		deserialisationCallback.accept(atom);
		submitAtom(atom);
		return atom.getAID();
	}

	@Override
	public String toString() {
		return String.format("%s[%x]", getClass().getSimpleName(), System.identityHashCode(this));
	}
}
