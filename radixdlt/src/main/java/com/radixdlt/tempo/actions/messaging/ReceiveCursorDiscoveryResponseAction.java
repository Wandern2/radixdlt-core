package com.radixdlt.tempo.actions.messaging;

import com.radixdlt.tempo.LogicalClockCursor;
import com.radixdlt.tempo.reactive.TempoAction;
import com.radixdlt.tempo.messages.CursorDiscoveryResponseMessage;
import com.radixdlt.tempo.store.CommitmentBatch;
import org.radix.network.peers.Peer;

import java.util.Objects;

public class ReceiveCursorDiscoveryResponseAction implements TempoAction {
	private final CommitmentBatch commitments;
	private final LogicalClockCursor cursor;
	private final Peer peer;

	public ReceiveCursorDiscoveryResponseAction(CommitmentBatch commitments, LogicalClockCursor cursor, Peer peer) {
		this.commitments = Objects.requireNonNull(commitments, "commitments is required");
		this.cursor = Objects.requireNonNull(cursor, "cursor is required");
		this.peer = Objects.requireNonNull(peer, "peer is required");
	}

	public CommitmentBatch getCommitments() {
		return commitments;
	}

	public LogicalClockCursor getCursor() {
		return cursor;
	}

	public Peer getPeer() {
		return peer;
	}

	public static ReceiveCursorDiscoveryResponseAction from(CursorDiscoveryResponseMessage message, Peer peer) {
		return new ReceiveCursorDiscoveryResponseAction(message.getCommitmentBatch(), message.getCursor(), peer);
	}
}
