package net.bluemind.metrics.registry.impl;

import java.util.Collections;
import java.util.concurrent.atomic.LongAdder;

import com.google.common.util.concurrent.RateLimiter;
import com.netflix.spectator.api.Clock;
import com.netflix.spectator.api.Counter;
import com.netflix.spectator.api.Id;
import com.netflix.spectator.api.Measurement;

import net.bluemind.metrics.registry.client.AgentPushClient;
import net.bluemind.metrics.registry.json.CounterJson;

public class BMCounter implements Counter {

	private final Clock clock;
	private final Id id;
	private final LongAdder count;
	private final AgentPushClient webSockClient;
	private final CounterJson dto;
	private final RateLimiter limiter;

	/** Create a new instance. */
	BMCounter(Clock clock, Id id, AgentPushClient webSockClient) {
		this.webSockClient = webSockClient;
		this.clock = clock;
		this.id = id;
		this.count = new LongAdder();
		this.dto = new CounterJson(id, 0L);
		this.limiter = RateLimiter.create(0.5);
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
		if (limiter.tryAcquire()) {
			webSockClient.queue(dto.withCount(count.longValue()));
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
