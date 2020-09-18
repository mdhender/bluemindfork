package net.bluemind.metrics.registry.impl;

import java.io.IOException;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.spectator.api.Clock;
import com.netflix.spectator.api.Gauge;
import com.netflix.spectator.api.Id;
import com.netflix.spectator.api.Measurement;
import com.netflix.spectator.impl.AtomicDouble;

import net.bluemind.metrics.registry.client.WebSocketClient;
import net.bluemind.metrics.registry.json.GaugeJson;

public class BMMaxGauge implements Gauge {
	private static final Logger logger = LoggerFactory.getLogger(BMMaxGauge.class);
	private final Clock clock;
	private final Id id;
	private final AtomicDouble value;
	private final WebSocketClient webSockClient;

	/** Create a new instance. */
	BMMaxGauge(Clock clock, Id id, WebSocketClient webSockClient) {
		this.webSockClient = webSockClient;
		this.clock = clock;
		this.id = id;
		this.value = new AtomicDouble(Double.NaN);
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
		value.max(v);
		try {
			GaugeJson gaugeJson = new GaugeJson(id, this.value.get());
			this.webSockClient.sendTextFrame(gaugeJson);
		} catch (IOException e) {
			logger.error("IOException : ", e);
		}
	}

	@Override
	public double value() {
		return value.get();
	}
}
