package net.bluemind.imap.endpoint.ratelimiter;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.annotations.VisibleForTesting;

import net.bluemind.imap.endpoint.EndpointConfig;
import net.bluemind.imap.endpoint.driver.MailboxConnection;

public class ThroughputLimiterRegistry {
	public enum Strategy {
		NONE, INTIME, BEHIND
	}

	private static final Logger logger = LoggerFactory.getLogger(ThroughputLimiterRegistry.class);

	private static ThroughputLimiterRegistry instance;

	public static synchronized ThroughputLimiterRegistry get(int initialCapacity) {
		if (instance == null) {
			instance = new ThroughputLimiterRegistry(initialCapacity);
		}
		return instance;
	}

	private final long capacity = EndpointConfig.get().getBytes("imap.throughput.capacity");
	private final Duration duration = EndpointConfig.get().getDuration("imap.throughput.period");
	private final int initialCapacity;
	private final Cache<String, ThroughputLimiter> limiters;

	public ThroughputLimiterRegistry(int initialCapacity) {
		logger.info("[throughput-limiter] strategy:{}, initial-capacity:{}", strategy().name(), initialCapacity);
		this.initialCapacity = initialCapacity;
		// Worst case scenario, we received 2 commands with literal of max size
		long expireInMs = 2 * (long) (initialCapacity * ((double) duration.toMillis() / capacity));
		this.limiters = Caffeine.newBuilder().expireAfterAccess(expireInMs, TimeUnit.MILLISECONDS).build();
	}

	public ThroughputLimiter get(MailboxConnection mailbox) {
		return Optional.ofNullable(mailbox) //
				.map(mb -> limiters.get(mb.login(), this::create)) //
				.orElseGet(() -> new NoopThroughputLimiter());
	}

	private static Strategy strategy() {
		try {
			return Strategy.valueOf(EndpointConfig.get().getString("imap.throughput.strategy").toUpperCase());
		} catch (IllegalArgumentException e) {
			return Strategy.BEHIND;
		}
	}

	private ThroughputLimiter create(String name) {
		switch (strategy()) {
		case NONE:
			return new NoopThroughputLimiter();
		case INTIME:
			return new InTimeThroughputLimiter(initialCapacity);
		case BEHIND:
		default:
			return new BehindThroughputLimiter(initialCapacity);
		}

	}

	@VisibleForTesting
	public static void clear() {
		instance.limiters.invalidateAll();
		instance.limiters.cleanUp();
	}

}
