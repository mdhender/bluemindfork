/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2019
 *
 * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License)
 * or the CeCILL as published by CeCILL.info (version 2 of the License).
 *
 * There are special exceptions to the terms and conditions of the
 * licenses as they are applied to this program. See LICENSE.txt in
 * the directory of this program distribution.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.lib.vertx.tests;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import org.junit.Assert;
import org.junit.Test;

import net.bluemind.lib.vertx.utils.Debouncer;

public class DebouncerTests {

	/** Successive calls are too close, only the last one should be done. */
	@Test
	public void testCallsBellowGracePeriod() {
		final int debounceGracePeriod = 50;
		final int successiveCallsDelay = 40;
		final int maxSuccesiveCalls = 100;

		final List<Integer> list = this.debounce(debounceGracePeriod, successiveCallsDelay, maxSuccesiveCalls);

		Assert.assertEquals(1, list.size());
		Assert.assertEquals(maxSuccesiveCalls - 1, list.get(0).intValue());
	}

	/**
	 * <i>NoDebounceFirst</i> is on: the first call should always be done. Other
	 * calls are too close, only the last one of them should be done.
	 */
	@Test
	public void testCallsBellowGracePeriodWithNoDebounceFirstMode() {
		final int debounceGracePeriod = 30;
		final int successiveCallsDelay = 20;
		final int maxSuccesiveCalls = 100;
		final boolean noDebounceFirst = true;

		final List<Integer> list = this.debounce(debounceGracePeriod, successiveCallsDelay, maxSuccesiveCalls,
				noDebounceFirst);

		Assert.assertEquals(2, list.size());
		Assert.assertEquals(0, list.get(0).intValue());
		Assert.assertEquals(maxSuccesiveCalls - 1, list.get(1).intValue());
	}

	/** Successive calls are away enough, all of them should be done. */
	@Test
	public void testCallsAboveGracePeriod() {
		final int debounceGracePeriod = 30;
		final int successiveCallsDelay = 40;
		final int maxSuccesiveCalls = 100;

		final List<Integer> list = this.debounce(debounceGracePeriod, successiveCallsDelay, maxSuccesiveCalls);

		Assert.assertEquals(100, list.size());
		for (int i = 0; i < maxSuccesiveCalls; i++) {
			Assert.assertEquals(i, list.get(i).intValue());
		}
	}

	/**
	 * <i>NoDebounceFirst</i> is on: the first call should always be done.
	 * Successive calls are away enough, all of them should be done.
	 */
	@Test
	public void testCallsAboveGracePeriodWithNoDebounceFirstMode() {
		final int debounceGracePeriod = 30;
		final int successiveCallsDelay = 40;
		final int maxSuccesiveCalls = 100;
		final boolean noDebounceFirst = true;

		final List<Integer> list = this.debounce(debounceGracePeriod, successiveCallsDelay, maxSuccesiveCalls,
				noDebounceFirst);

		Assert.assertEquals(100, list.size());
		for (int i = 0; i < maxSuccesiveCalls; i++) {
			Assert.assertEquals(i, list.get(i).intValue());
		}
	}

	private List<Integer> debounce(final int debounceGracePeriod, final int successiveCallsDelay,
			final int maxSuccesiveCalls) {
		return this.debounce(debounceGracePeriod, successiveCallsDelay, maxSuccesiveCalls, false);
	}

	private List<Integer> debounce(final int debounceGracePeriod, final int successiveCallsDelay,
			final int maxSuccesiveCalls, final boolean noDebounceFirst) {
		final List<Integer> list = new ArrayList<>();
		final Debouncer<String, Integer> debouncer = new Debouncer<>(new BiConsumer<String, Integer>() {

			@Override
			public void accept(final String key, final Integer payload) {
				// register "toto" calls
				list.add(payload);
			}
		}, debounceGracePeriod, noDebounceFirst);

		for (int i = 0; i < maxSuccesiveCalls; i++) {
			debouncer.call("toto", i);
			try {
				Thread.sleep(successiveCallsDelay);
			} catch (InterruptedException e) {
			}
		}

		// add more time to reach the grace period end
		try {
			Thread.sleep(debounceGracePeriod);
		} catch (InterruptedException e) {
		}

		return list;
	}

	/** Calls with different debounce keys should not interfere with each other. */
	@Test
	public void testCallsWithDifferentIds() {
		this.callsWithDifferentIds(false);
	}

	/**
	 * <i>NoDebounceFirst</i> is on: the first call should always be done. Calls
	 * with different debounce keys should not interfere with each other.
	 */
	@Test
	public void testCallsWithDifferentIdsWithNoDebounceFirstMode() {
		this.callsWithDifferentIds(true);
	}

	private void callsWithDifferentIds(final boolean noDebounceFirst) {
		final int debounceGracePeriod = 30;
		final int successiveCallsDelay = 20;
		final int successiveCallsDelay2 = 40;
		final int maxSuccesiveCalls = 100;

		final List<Integer> list = new ArrayList<>();
		final List<Integer> list2 = new ArrayList<>();
		final Debouncer<String, Integer> debouncer = new Debouncer<>(new BiConsumer<String, Integer>() {

			@Override
			public void accept(final String key, final Integer payload) {
				if (key.equals("toto")) {
					// register "toto" calls
					list.add(payload);
				} else {
					// register "tata" calls
					list2.add(payload);
				}
			}
		}, debounceGracePeriod, noDebounceFirst);

		final Thread thread = new Thread(new Runnable() {

			@Override
			public void run() {
				for (int i = 0; i < maxSuccesiveCalls; i++) {
					debouncer.call("toto", i);
					try {
						Thread.sleep(successiveCallsDelay);
					} catch (InterruptedException e) {
					}
				}

				// add more time to reach the grace period end
				try {
					final int sleep = debounceGracePeriod - successiveCallsDelay;
					Thread.sleep(sleep);
				} catch (InterruptedException e) {
				}

			}
		});
		final Thread thread2 = new Thread(new Runnable() {

			@Override
			public void run() {
				for (int i = 0; i < maxSuccesiveCalls; i++) {
					debouncer.call("tata", i);
					try {
						Thread.sleep(successiveCallsDelay2);
					} catch (InterruptedException e) {
					}
				}
			}
		});

		thread.start();
		thread2.start();
		try {
			thread.join();
			thread2.join();
		} catch (InterruptedException e) {
		}

		if (noDebounceFirst) {
			// the first and the last "toto" calls should be done
			Assert.assertEquals(2, list.size());
			Assert.assertEquals(0, list.get(0).intValue());
			Assert.assertEquals(maxSuccesiveCalls - 1, list.get(1).intValue());
		} else {
			// only the last "toto" call
			Assert.assertEquals(1, list.size());
			Assert.assertEquals(maxSuccesiveCalls - 1, list.get(0).intValue());
		}

		// all "tata" calls (since the calls are away enough, the noDebounceFirst mode
		// has no impact)
		Assert.assertEquals(100, list2.size());
		Assert.assertEquals(0, list2.get(0).intValue());
		Assert.assertEquals(50, list2.get(50).intValue());
		Assert.assertEquals(99, list2.get(99).intValue());
	}

}
