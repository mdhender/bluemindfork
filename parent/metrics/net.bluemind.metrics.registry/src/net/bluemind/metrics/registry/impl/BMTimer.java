package net.bluemind.metrics.registry.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.spectator.api.AbstractTimer;
import com.netflix.spectator.api.Clock;
import com.netflix.spectator.api.Id;
import com.netflix.spectator.api.Measurement;
import com.netflix.spectator.api.Statistic;

import net.bluemind.metrics.registry.client.WebSocketClient;
import net.bluemind.metrics.registry.json.TimerJson;

public class BMTimer extends AbstractTimer {

	private static final Logger logger = LoggerFactory.getLogger(BMTimer.class);

	private final WebSocketClient webSockClient;
	private final Id id;
	private final LongAdder count;
	private final LongAdder totalTime;
	private final TimerJson dto;

	/** Create a new instance. */
	BMTimer(Clock clock, Id id, WebSocketClient webSockClient) {
		super(clock);
		this.webSockClient = webSockClient;
		this.id = id;
		count = new LongAdder();
		totalTime = new LongAdder();
		this.dto = new TimerJson(id, 0);
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
	public void record(long amount, TimeUnit unit) {
		if (amount >= 0) {
			final long nanos = TimeUnit.NANOSECONDS.convert(amount, unit);
			totalTime.add(nanos);
			count.increment();
			webSockClient.queue(dto.withNanos(nanos));
		}
	}

	@Override
	public Iterable<Measurement> measure() {
		final long now = clock.wallTime();
		final List<Measurement> ms = new ArrayList<>(2);
		ms.add(new Measurement(id.withTag(Statistic.count), now, count.longValue()));
		ms.add(new Measurement(id.withTag(Statistic.totalTime), now, totalTime.longValue()));
		return ms;
	}

	@Override
	public long count() {
		return count.longValue();
	}

	@Override
	public long totalTime() {
		return totalTime.longValue();
	}
}
