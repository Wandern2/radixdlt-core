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

package com.radixdlt.consensus.liveness;

import com.radixdlt.consensus.NewView;
import com.radixdlt.consensus.View;
import com.radixdlt.consensus.safety.QuorumRequirements;
import com.radixdlt.crypto.ECDSASignature;
import com.radixdlt.crypto.ECDSASignatures;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Overly simplistic pacemaker
 */
public final class PacemakerImpl implements Pacemaker, PacemakerRx {
	static final int TIMEOUT_MILLISECONDS = 500;
	private final PublishSubject<View> timeouts;
	private final ScheduledExecutorService executorService;
	private final QuorumRequirements quorumRequirements;

	private final Map<View, ECDSASignatures> pendingNewViews = new HashMap<>();
	private View currentView = View.of(0L);
	private View highestQCView = View.of(0L);

	public PacemakerImpl(QuorumRequirements quorumRequirements, ScheduledExecutorService executorService) {
		this.quorumRequirements = Objects.requireNonNull(quorumRequirements);
		this.executorService = Objects.requireNonNull(executorService);
		this.timeouts = PublishSubject.create();
	}

	private void scheduleTimeout(final View timeoutView) {
		executorService.schedule(() -> {
			timeouts.onNext(timeoutView);
		}, TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS);
	}

	@Override
	public View getCurrentView() {
		return currentView;
	}

	@Override
	public boolean processLocalTimeout(View view) {
		if (!view.equals(this.currentView)) {
			return false;
		}

		this.currentView = currentView.next();

		scheduleTimeout(this.currentView);
		return true;
	}

	@Override
	public Optional<View> processRemoteNewView(NewView newView) {
		ECDSASignature signature = newView.getSignature().orElseThrow(() -> new IllegalArgumentException("new-view is missing signature"));
		ECDSASignatures signatures = pendingNewViews.getOrDefault(newView.getView(), new ECDSASignatures());

		// try to add the signature if permitted by the requirements
		if (quorumRequirements.accepts(newView.getAuthor().getUID())) {
			// FIXME ugly cast to ECDSASignatures because we need a specific type
			// TODO do we even need to keep signatures or just QCs & count for new-views?
			signatures = (ECDSASignatures) signatures.concatenate(newView.getAuthor(), signature);
		} else {
			// there is no meaningful inaction here, so better let the caller know
			throw new IllegalArgumentException("new-view " + newView + " was not accepted");
		}

		// check if we have gotten enough new-views to proceed
		if (signatures.count() >= quorumRequirements.numRequiredVotes()) {
			// if we got enough new-views, remove pending and return formed QC
			pendingNewViews.remove(newView.getView());
			return Optional.of(newView.getView().next());
		} else {
			// if we haven't got enough new-views yet, do nothing
			pendingNewViews.put(newView.getView(), signatures);
			return Optional.empty();
		}
	}

	private void updateHighestQCView(View view) {
		if (view.compareTo(highestQCView) > 0) {
			highestQCView = view;
		}
	}

	@Override
	public Optional<View> processQC(View view) {
		// update
		updateHighestQCView(view);

		// check if a new view can be started
		View newView = highestQCView.next();
		if (newView.compareTo(currentView) <= 0) {
			return Optional.empty();
		}

		// start new view
		this.currentView = newView;

		scheduleTimeout(this.currentView);

		return Optional.of(this.currentView);
	}

	@Override
	public void start() {
		scheduleTimeout(this.currentView);
	}

	@Override
	public Observable<View> localTimeouts() {
		return timeouts;
	}
}
