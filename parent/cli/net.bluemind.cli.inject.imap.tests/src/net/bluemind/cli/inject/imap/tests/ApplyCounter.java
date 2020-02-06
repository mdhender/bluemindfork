/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2019
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.cli.inject.imap.tests;

import java.util.concurrent.atomic.LongAdder;

import io.vertx.core.Vertx;
import net.bluemind.backend.cyrus.replication.observers.IReplicationObserver;
import net.bluemind.backend.cyrus.replication.observers.IReplicationObserverProvider;

public class ApplyCounter implements IReplicationObserverProvider {
	private static final LongAdder adder = new LongAdder();

	private static final IReplicationObserver INST = new IReplicationObserver() {

		@Override
		public void onApplyMessages(int total) {
			adder.add(total);
		}

		@Override
		public void onApplyMailbox(String mboxUniqueId) {
		}
	};

	public static void reset() {
		adder.reset();
	}

	public static int total() {
		return (int) adder.sum();
	}

	@Override
	public IReplicationObserver create(Vertx vertx) {
		return INST;
	}

}
