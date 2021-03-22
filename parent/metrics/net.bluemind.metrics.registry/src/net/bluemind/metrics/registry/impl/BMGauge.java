package net.bluemind.metrics.registry.impl;

import java.util.Collections;

import com.google.common.util.concurrent.RateLimiter;
import com.netflix.spectator.api.Clock;
import com.netflix.spectator.api.Gauge;
import com.netflix.spectator.api.Id;
import com.netflix.spectator.api.Measurement;
import com.netflix.spectator.impl.AtomicDouble;

import net.bluemind.metrics.registry.client.AgentPushClient;
import net.bluemind.metrics.registry.json.GaugeJson;

public class BMGauge implements Gauge {

	private final Clock clock;
	private final Id id;
	private final AtomicDouble value;
	private final AgentPushClient webSockClient;
	private final RateLimiter limiter;

	/** Create a new instance. */
	BMGauge(Clock clock, Id id, AgentPushClient webSockClient) {
		this.webSockClient = webSockClient;
		this.clock = clock;
		this.id = id;
		this.value = new AtomicDouble(0);
		this.limiter = RateLimiter.create(0.5);
	}

	@Override
	public Id id() {
		return id;
	}

	@Override
	public Iterable<Measurement> measure() {
		final Measurement m = new Measurement(id, clock.wallTime(), value());
		return Collections.singletonList(m);
	}

	@Override
	public boolean hasExpired() {
		return false;
	}

	@Override
	public void set(double v) {
		value.set(v);
		if (limiter.tryAcquire()) {
			GaugeJson gaugeJson = new GaugeJson(id, value());
			webSockClient.queue(gaugeJson);
		}
	}

	@Override
	public double value() {
		return value.get();
	}
}
