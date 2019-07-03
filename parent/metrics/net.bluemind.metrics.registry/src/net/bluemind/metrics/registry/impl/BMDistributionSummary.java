package net.bluemind.metrics.registry.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.LongAdder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.spectator.api.Clock;
import com.netflix.spectator.api.DistributionSummary;
import com.netflix.spectator.api.Id;
import com.netflix.spectator.api.Measurement;
import com.netflix.spectator.api.Statistic;

import net.bluemind.metrics.registry.client.WebSocketClient;
import net.bluemind.metrics.registry.json.DistributionSummaryJson;

public class BMDistributionSummary implements DistributionSummary {
	private static final Logger logger = LoggerFactory.getLogger(BMDistributionSummary.class);
	private final Clock clock;
	private final Id id;
	private final LongAdder count;
	private final LongAdder totalAmount;
	private final WebSocketClient webSockClient;

	/** Create a new instance. */
	BMDistributionSummary(Clock clock, Id id, WebSocketClient webSockClient) {
		this.webSockClient = webSockClient;
		this.clock = clock;
		this.id = id;
		count = new LongAdder();
		totalAmount = new LongAdder();
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
	public void record(long amount) {
		if (amount >= 0) {
			totalAmount.add(amount);
			count.increment();
		}
		try {
			DistributionSummaryJson distributionSummaryJson = new DistributionSummaryJson(id, amount);
			this.webSockClient.sendTextFrame(Mapper.get().writeValueAsString(distributionSummaryJson));
		} catch (IOException e) {
			logger.error("IOException : ", e);
		}
	}

	@Override
	public Iterable<Measurement> measure() {
		final long now = clock.wallTime();
		final List<Measurement> ms = new ArrayList<>(2);
		ms.add(new Measurement(id.withTag(Statistic.count), now, count.longValue()));
		ms.add(new Measurement(id.withTag(Statistic.totalAmount), now, totalAmount.longValue()));
		return ms;
	}

	@Override
	public long count() {
		return count.longValue();
	}

	@Override
	public long totalAmount() {
		return totalAmount.longValue();
	}
}
