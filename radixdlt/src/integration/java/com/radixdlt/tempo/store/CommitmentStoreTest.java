package com.radixdlt.tempo.store;

import com.google.common.collect.ImmutableList;
import com.radixdlt.common.EUID;
import com.radixdlt.crypto.CryptoException;
import com.radixdlt.crypto.Hash;
import com.radixdlt.serialization.Serialization;
import com.radixdlt.tempo.AtomGenerator;
import org.assertj.core.api.SoftAssertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.radix.database.DatabaseEnvironment;
import org.radix.exceptions.ValidationException;
import org.radix.integration.RadixTestWithStores;
import org.radix.logging.Logger;
import org.radix.logging.Logging;
import org.radix.modules.Modules;
import org.radix.universe.system.LocalSystem;
import org.radix.utils.SystemProfiler;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assume.assumeTrue;

public class CommitmentStoreTest extends RadixTestWithStores {

    private static final Logger LOGGER = Logging.getLogger("TempoAtomStoreTests");

    private AtomGenerator atomGenerator = new AtomGenerator();
    private LocalSystem localSystem = LocalSystem.getInstance();
    private Serialization serialization = Serialization.getDefault();
    private SystemProfiler profiler = SystemProfiler.getInstance();
    private CommitmentStore commitmentStore;

    @Before
    public void setup() throws CryptoException, ValidationException {
        commitmentStore = new CommitmentStore(() -> Modules.get(DatabaseEnvironment.class));
        commitmentStore.reset();
        commitmentStore.open();
    }

    @After
    public void teardown() {
        commitmentStore.close();
    }

    @Test
    public void testSingleIdentity() {
        SoftAssertions.assertSoftly(softly -> {
            testStoreRetrieveFor(EUID.ONE, softly, 0, 200);
        });
    }

    private void testStoreRetrieveFor(EUID self, SoftAssertions softly, int offset, int count) {
        for (int i = offset; i < count; i++) {
            Hash commitment = Hash.random();
            commitmentStore.put(self, i, commitment);
            ImmutableList<Hash> next = commitmentStore.getNext(self, i - 1, 1);
            softly.assertThat(next).containsExactly(commitment);
        }
    }

    private void testBatchStoreRetrieveFor(EUID self, SoftAssertions softly, int offset, int count) {
        List<Hash> commitments = Stream.generate(Hash::random)
            .limit(count)
            .collect(Collectors.toList());
        commitmentStore.put(self, commitments, offset);
        ImmutableList<Hash> next = commitmentStore.getNext(self, offset - 1, count);
        softly.assertThat(next).containsExactlyElementsOf(commitments);
    }

    @Test
    public void testTwoIdentities() {
        SoftAssertions.assertSoftly(softly -> {
            testStoreRetrieveFor(EUID.ONE, softly, 0, 3);
            testBatchStoreRetrieveFor(EUID.ONE, softly, 3, 3);
            testStoreRetrieveFor(EUID.TWO, softly, 0, 3);
        });
    }
}
