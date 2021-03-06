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

package com.radixdlt.consensus.validators;

import org.junit.Test;
import org.radix.serialization.SerializeObject;

import com.radixdlt.crypto.CryptoException;
import com.radixdlt.crypto.ECKeyPair;
import nl.jqno.equalsverifier.EqualsVerifier;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class ValidatorTest extends SerializeObject<Validator> {

	public ValidatorTest() {
		super(Validator.class, ValidatorTest::create);
	}

	@Test
	public void equalsContract() {
		EqualsVerifier.forClass(Validator.class)
			.verify();
	}

	@Test
	public void sensibleToString() {
		String s = create().toString();
		assertThat(s, containsString(Validator.class.getSimpleName()));
	}

	@Test
	public void testGetter() {
		assertNotNull(create().nodeKey());
	}

	private static Validator create() {
		try {
			ECKeyPair nodeKey = new ECKeyPair();
			return Validator.from(nodeKey.getPublicKey());
		} catch (CryptoException e) {
			throw new IllegalStateException("While constructing validator", e);
		}
	}
}
