/*
 *  (C) Copyright 2020 Radix DLT Ltd
 *
 *  Radix DLT Ltd licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except in
 *  compliance with the License.  You may obtain a copy of the
 *  License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 *  either express or implied.  See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package com.radixdlt.consensus;

import com.google.inject.Inject;

import java.util.HashSet;
import java.util.Optional;

/**
 * Manages the BFT Vertex chain
 */
public final class VertexStore {
	private final HashSet<Vertex> vertices = new HashSet<>();
	private QuorumCertificate highestQC = null;

	@Inject
	public VertexStore() {
	}

	public void syncToQC(QuorumCertificate qc) {
		if (qc == null) {
			return;
		}

		if (highestQC == null || highestQC.getRound().compareTo(qc.getRound()) < 0) {
			highestQC = qc;
		}
	}

	public void insertVertex(Vertex vertex) {
		this.syncToQC(vertex.getQC());
		vertices.add(vertex);
	}

	public Optional<QuorumCertificate> getHighestQC() {
		return Optional.ofNullable(this.highestQC);
	}
}