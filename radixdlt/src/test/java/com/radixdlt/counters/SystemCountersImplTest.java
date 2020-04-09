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

package com.radixdlt.counters;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.Map;

import org.junit.Test;

import com.radixdlt.counters.SystemCounters.CounterType;

public class SystemCountersImplTest {
	@Test
	public void when_get_count__then_count_should_be_0() {
		SystemCounters counters = new SystemCountersImpl();
		assertThat(counters.get(CounterType.CONSENSUS_TIMEOUT)).isEqualTo(0L);
	}

	@Test
	public void when_increment__then_count_should_be_1() {
		SystemCounters counters = new SystemCountersImpl();
		counters.increment(CounterType.CONSENSUS_TIMEOUT);
		assertThat(counters.get(CounterType.CONSENSUS_TIMEOUT)).isEqualTo(1L);
		counters.increment(CounterType.CONSENSUS_TIMEOUT);
		assertThat(counters.get(CounterType.CONSENSUS_TIMEOUT)).isEqualTo(2L);
	}

	@Test
	public void when_add__then_count_should_be_added_value() {
		SystemCounters counters = new SystemCountersImpl();
		counters.add(CounterType.CONSENSUS_TIMEOUT, 1234);
		assertThat(counters.get(CounterType.CONSENSUS_TIMEOUT)).isEqualTo(1234L);
		counters.add(CounterType.CONSENSUS_TIMEOUT, 4321);
		assertThat(counters.get(CounterType.CONSENSUS_TIMEOUT)).isEqualTo(1234L + 4321L);
	}

	@Test
	public void when_set__then_count_should_be_1() {
		SystemCounters counters = new SystemCountersImpl();
		counters.set(CounterType.CONSENSUS_TIMEOUT, 1234);
		assertThat(counters.get(CounterType.CONSENSUS_TIMEOUT)).isEqualTo(1234L);
		counters.set(CounterType.CONSENSUS_TIMEOUT, 4321);
		assertThat(counters.get(CounterType.CONSENSUS_TIMEOUT)).isEqualTo(4321L);
	}

	@Test
	public void when_tomap__then_values_correct() {
		SystemCounters counters = new SystemCountersImpl();
		for (CounterType value : CounterType.values()) {
			counters.set(value, value.ordinal() + 1L);
		}
		Map<String, Object> m = counters.toMap();
		testMap("", m);
	}

	@Test
	public void sensible_tostring() {
		SystemCounters counters = new SystemCountersImpl();
		counters.set(CounterType.CONSENSUS_TIMEOUT, 1234);
		String s = counters.toString();
		assertThat(s).contains(SystemCountersImpl.class.getSimpleName());
		assertThat(s).contains("1234");
	}

	private void testMap(String path, Map<String, Object> m) {
		for (Map.Entry<String, Object> entry : m.entrySet()) {
			String p = entry.getKey().toUpperCase();
			String newPath = path.isEmpty() ? p : path + "_" + p;
			Object o = entry.getValue();
			if (o instanceof Map<?, ?>) {
				@SuppressWarnings("unchecked")
				Map<String, Object> newm = (Map<String, Object>) o;
				testMap(newPath, newm);
			} else {
				String s = (String) o;
				CounterType ct = CounterType.valueOf(newPath);
				assertThat(Long.parseLong(s)).isEqualTo(ct.ordinal() + 1L);
			}
		}
	}
}