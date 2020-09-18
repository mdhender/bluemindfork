package net.bluemind.metrics.registry.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.atomic.LongAdder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.spectator.api.Clock;
import com.netflix.spectator.api.Counter;
import com.netflix.spectator.api.Id;
import com.netflix.spectator.api.Measurement;

import net.bluemind.metrics.registry.client.WebSocketClient;
import net.bluemind.metrics.registry.json.CounterJson;

public class BMCounter implements Counter {

	private static final Logger logger = LoggerFactory.getLogger(BMCounter.class);

	private final Clock clock;
	private final Id id;
	private final LongAdder count;
	private final WebSocketClient webSockClient;
	private final CounterJson dto;

	/** Create a new instance. */
	BMCounter(Clock clock, Id id, WebSocketClient webSockClient) {
		this.webSockClient = webSockClient;
		this.clock = clock;
		this.id = id;
		this.count = new LongAdder();
		this.dto = new CounterJson(id, 0L);
	}

	@Override
	public Id id() {
		return id;
	}

	@Override
	public boolean hasExpired() {
		return false;
	}

	@Override
	public Iterable<Measurement> measure() {
		long now = clock.wallTime();
		long v = count.longValue();
		return Collections.singleton(new Measurement(id, now, v));
	}

	@Override
	public void increment() {
		count.increment();
		propagateUpdate();
	}

	private void propagateUpdate() {
		try {
			webSockClient.sendTextFrame(dto.withCount(count.longValue()));
		} catch (IOException e) {
			logger.error("IOException", e);
		}
	}

	@Override
	public void increment(long amount) {
		count.add(amount);
		propagateUpdate();
	}

	@Override
	public long count() {
		return count.longValue();
	}

	@Override
	public void add(double amount) {
		// We only use long but it looks like this is the only method called.
		count.add((long) amount);
		propagateUpdate();
	}

	@Override
	public double actualCount() {
		return count.longValue();
	}
}
