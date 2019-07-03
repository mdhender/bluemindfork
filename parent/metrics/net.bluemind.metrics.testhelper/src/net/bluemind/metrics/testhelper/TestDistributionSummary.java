package net.bluemind.metrics.testhelper;

import java.util.concurrent.atomic.LongAdder;

import com.netflix.spectator.api.DistributionSummary;
import com.netflix.spectator.api.Id;
import com.netflix.spectator.api.Measurement;

public class TestDistributionSummary implements DistributionSummary {
	private LongAdder count = new LongAdder();
	private LongAdder totalAmount = new LongAdder();
	private Id id;

	public TestDistributionSummary(Id id) {
		this.count = new LongAdder();
		this.id = id;
	}

	@Override
	public boolean hasExpired() {
		return false;
	}

	@Override
	public Id id() {
		return this.id;
	}

	@Override
	public Iterable<Measurement> measure() {
		return null;
	}

	@Override
	public long count() {
		return this.count.longValue();
	}

	@Override
	public void record(long amount) {
		if (amount >= 0) {
			this.totalAmount.add(amount);
			count.increment();
		}
	}

	@Override
	public long totalAmount() {
		return this.totalAmount.longValue();
	}
}
