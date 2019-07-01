package net.bluemind.metrics.registry.impl;

import java.io.FileNotFoundException;
import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.spectator.api.AbstractRegistry;
import com.netflix.spectator.api.Clock;
import com.netflix.spectator.api.Counter;
import com.netflix.spectator.api.DistributionSummary;
import com.netflix.spectator.api.Gauge;
import com.netflix.spectator.api.Id;
import com.netflix.spectator.api.Timer;

import net.bluemind.metrics.registry.client.WebSocketClient;

public class BMRegistry extends AbstractRegistry {
	private static final Logger logger = LoggerFactory.getLogger(BMRegistry.class);
	private final WebSocketClient webSockClient;

	public BMRegistry() throws FileNotFoundException, URISyntaxException {
		super(Clock.SYSTEM);
		webSockClient = new WebSocketClient();
		try {
			webSockClient.open();
		} catch (InterruptedException e) {
			logger.info("Metrics server interrupted {}", e);
		}
	}

	@Override
	protected Counter newCounter(Id id) {
		return new BMCounter(clock(), id, this.webSockClient);
	}

	@Override
	protected DistributionSummary newDistributionSummary(Id id) {
		return new BMDistributionSummary(clock(), id, this.webSockClient);
	}

	@Override
	protected Timer newTimer(Id id) {
		return new BMTimer(clock(), id, this.webSockClient);
	}

	@Override
	protected Gauge newGauge(Id id) {
		return new BMGauge(clock(), id, this.webSockClient);
	}

	@Override
	protected Gauge newMaxGauge(Id id) {
		return new BMMaxGauge(clock(), id, this.webSockClient);
	}
}
