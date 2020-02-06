package net.bluemind.metrics.registry;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.spectator.api.Registry;
import com.netflix.spectator.api.Spectator;

import net.bluemind.metrics.registry.impl.BMRegistry;

public class MetricsRegistry {

	private static final Logger logger = LoggerFactory.getLogger(MetricsRegistry.class);
	private static boolean available = false;

	static {
		if (new File("/etc/bm/metrics.disabled").exists()) {
			logger.warn("Agent/Metrics disabled by /etc/bm/metrics.disabled");
		} else {
			tryInit();
		}
	}

	private MetricsRegistry() {
	}

	public static Registry get() {
		return Spectator.globalRegistry();
	}

	private static void tryInit() {
		try {
			Registry registry = new BMRegistry();
			Spectator.globalRegistry().add(registry);
			logger.info("Agent connection established");
			MetricsRegistry.setAvailable(true);
		} catch (Exception e) {
			logger.error("Agent connection is not available: {}", e.getMessage());
		}
	}

	public static boolean isAvailable() {
		return available;
	}

	public static void setAvailable(boolean b) {
		available = b;
	}

}
