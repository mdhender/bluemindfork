/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.metrics.testhelper;

import java.util.concurrent.atomic.LongAdder;

import com.netflix.spectator.api.Counter;
import com.netflix.spectator.api.Id;
import com.netflix.spectator.api.Measurement;

public class TestCounter implements Counter {

	private LongAdder count;
	private Id id;

	public TestCounter(TestRegistry testRegistry, Id arg0) {
		this.count = new LongAdder();
		this.id = arg0;
	}

	@Override
	public boolean hasExpired() {
		return false;
	}

	@Override
	public Id id() {
		return id;
	}

	@Override
	public Iterable<Measurement> measure() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long count() {
		return count.sum();
	}

	@Override
	public void increment() {
		count.increment();

	}

	@Override
	public void increment(long arg0) {
		count.add(arg0);
	}

	@Override
	public void add(double amount) {
		increment((long)amount);
	}

	@Override
	public double actualCount() {
		return count.sum();
	}

}
