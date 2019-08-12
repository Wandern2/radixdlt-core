package org.radix.network2.messaging;

import java.io.Closeable;

import org.radix.network2.addressbook.Peer;
import org.radix.network2.transport.Transport;

public interface ConnectionManager extends Closeable {

	Transport findTransport(Peer peer, byte[] bytes);

}
