package net.bluemind.metrics.registry.impl;

import java.util.Collections;

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

	/** Create a new instance. */
	BMGauge(Clock clock, Id id, AgentPushClient webSockClient) {
		this.webSockClient = webSockClient;
		this.clock = clock;
		this.id = id;
		this.value = new AtomicDouble(0);
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
		GaugeJson gaugeJson = new GaugeJson(id, this.value.get());
		this.webSockClient.queue(gaugeJson);
	}

	@Override
	public double value() {
		return value.get();
	}
}
