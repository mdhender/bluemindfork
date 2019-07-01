package net.bluemind.metrics.testhelper;

import com.netflix.spectator.api.Gauge;
import com.netflix.spectator.api.Id;
import com.netflix.spectator.api.Measurement;
import com.netflix.spectator.impl.AtomicDouble;

public class TestGauge implements Gauge {
	private final Id id;
	private final AtomicDouble value;

	public TestGauge(Id id) {
		this.id = id;
		this.value = new AtomicDouble(Double.NaN);
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
		return null;
	}

	@Override
	public void set(double v) {
		value.set(v);
	}

	@Override
	public double value() {
		return value.doubleValue();
	}

}
