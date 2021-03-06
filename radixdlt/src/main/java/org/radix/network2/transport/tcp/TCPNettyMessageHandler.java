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

package org.radix.network2.transport.tcp;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.radix.logging.Logger;
import org.radix.logging.Logging;
import org.radix.network2.messaging.InboundMessage;
import org.radix.network2.messaging.InboundMessageConsumer;
import org.radix.network2.transport.StaticTransportMetadata;
import org.radix.network2.transport.TransportInfo;

import com.google.common.util.concurrent.RateLimiter;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

// For now we are just going to queue up the messages here.
// In the longer term, we would dispatch the messages in the netty group as well,
// but I think that would be problematic right now, as there is undoubtedly some
// downstream blocking that will happen, and this has the potential to slow down
// the whole inbound network pipeline.
final class TCPNettyMessageHandler extends SimpleChannelInboundHandler<ByteBuf> {
	private static final Logger log = Logging.getLogger("transport.tcp");

	private final InboundMessageConsumer messageSink;

	// Limit rate at which bad SocketAddress type logs are produced
	private final RateLimiter logRateLimiter = RateLimiter.create(1.0);


	TCPNettyMessageHandler(InboundMessageConsumer messageSink) {
		this.messageSink = messageSink;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, ByteBuf buf) throws Exception {
		SocketAddress socketSender = ctx.channel().remoteAddress();
		if (socketSender instanceof InetSocketAddress) {
			InetSocketAddress sender = (InetSocketAddress) socketSender;

			final int length = buf.readableBytes();
			final byte[] data = new byte[length];
			buf.readBytes(data);
			TransportInfo source = TransportInfo.of(
				TCPConstants.TCP_NAME,
				StaticTransportMetadata.of(
					TCPConstants.METADATA_TCP_HOST, sender.getAddress().getHostAddress(),
					TCPConstants.METADATA_TCP_PORT, String.valueOf(sender.getPort())
				)
			);
			messageSink.accept(InboundMessage.of(source, data));
		} else if (logRateLimiter.tryAcquire()) {
			String type = socketSender == null ? null : socketSender.getClass().getName();
			String from = socketSender == null ? null : socketSender.toString();
			log.error(String.format("Unknown SocketAddress of type %s from %s", type, from));
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		log.error("While receiving TCP packet", cause);
	}
}