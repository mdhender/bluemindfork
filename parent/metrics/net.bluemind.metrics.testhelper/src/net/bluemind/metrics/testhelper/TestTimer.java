package net.bluemind.metrics.testhelper;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

import com.netflix.spectator.api.AbstractTimer;
import com.netflix.spectator.api.Clock;
import com.netflix.spectator.api.Id;
import com.netflix.spectator.api.Measurement;

public class TestTimer extends AbstractTimer {
	private LongAdder totalTime = new LongAdder();
	private LongAdder count = new LongAdder();
	private Id id;

	public TestTimer(Id id) {
		super(Clock.SYSTEM);
		this.id = id;
	}

	@Override
	public long count() {
		return count.longValue();
	}

	@Override
	public void record(long amount, TimeUnit unit) {
		if (amount >= 0) {
			final long nanos = TimeUnit.NANOSECONDS.convert(amount, unit);
			this.totalTime.add(nanos);
			count.increment();
		}
	}

	@Override
	public long totalTime() {
		return this.totalTime.longValue();
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
		// TODO Auto-generated method stub
		return null;
	}
}
