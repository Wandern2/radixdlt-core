package com.radixdlt.tempo.store.legacy;

import com.radixdlt.common.AID;
import com.radixdlt.ledger.LedgerCursor;
import org.bouncycastle.util.Arrays;
import org.radix.atoms.AtomStore;
import org.radix.modules.Modules;

import java.io.IOException;
import java.util.Objects;

public class LegacyCursor implements LedgerCursor {
	private LedgerIndexType type;
	private byte[] primary;
	private byte[] index;

	public LegacyCursor(LedgerIndexType type, byte[] primary, byte[] index) {
		this.type = type;
		this.primary = Arrays.clone(Objects.requireNonNull(primary));
		this.index = Arrays.clone(Objects.requireNonNull(index));
	}

	@Override
	public LedgerIndexType getType() {
		return this.type;
	}

	public byte[] getPrimary() {
		return this.primary;
	}

	public byte[] getIndex() {
		return this.index;
	}

	@Override
	public AID get() {
		return AID.from(this.primary, Long.BYTES);
	}

	@Override
	public LedgerCursor next() throws IOException {
		return Modules.get(AtomStore.class).getNext(this);
	}

	@Override
	public LedgerCursor previous() throws IOException {
		return Modules.get(AtomStore.class).getPrev(this);
	}

	@Override
	public LedgerCursor first() throws IOException {
		return Modules.get(AtomStore.class).getFirst(this);
	}

	@Override
	public LedgerCursor last() throws IOException {
		return Modules.get(AtomStore.class).getLast(this);
	}
}