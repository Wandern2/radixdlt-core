package org.radix.network2.transport;

import java.io.Closeable;
import java.util.concurrent.CompletableFuture;

/**
 * Interface providing control over a transport's outbound connections.
 */
public interface TransportControl extends Closeable {

	/**
	 * Open an outbound connection to a peer.
	 *
	 * @param endpointMetadata the metadata for the endpoint we are connecting to
	 * @return A {@link CompletableFuture} returning an outbound transport connection once the connection is open
	 */
	CompletableFuture<TransportOutboundConnection> open(TransportMetadata endpointMetadata);

}
