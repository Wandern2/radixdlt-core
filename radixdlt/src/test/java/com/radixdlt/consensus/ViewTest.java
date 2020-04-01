package com.radixdlt.consensus;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ViewTest {
	@Test
	public void testPrevious() {
		assertThat(View.of(2L).previous()).isEqualTo(View.of(1L));
		assertThatThrownBy(() -> View.of(0L).previous());
	}

	@Test
	public void testNext() {
		assertThat(View.of(1L).next()).isEqualTo(View.of(2L));
		assertThatThrownBy(() -> View.of(Long.MAX_VALUE).next());
	}

	@Test
	public void equalsContract() {
		EqualsVerifier.forClass(View.class)
			.verify();
	}
}