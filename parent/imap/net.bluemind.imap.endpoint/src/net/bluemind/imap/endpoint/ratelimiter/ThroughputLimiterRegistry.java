package net.bluemind.imap.endpoint.ratelimiter;

import java.time.Duration;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.annotations.VisibleForTesting;
import com.typesafe.config.Config;

import net.bluemind.configfile.ConfigChangeListener;
import net.bluemind.configfile.imap.ImapConfig;
import net.bluemind.imap.endpoint.EndpointConfig;
import net.bluemind.imap.endpoint.driver.MailboxConnection;

public class ThroughputLimiterRegistry implements ConfigChangeListener {
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

	private final int initialCapacity;
	private final Cache<String, ThroughputLimiter> limiters;
	private Strategy strategy;
	private Set<String> bypass;

	public ThroughputLimiterRegistry(int initialCapacity) {
		this.initialCapacity = initialCapacity;
		long expirationInMs = setupConfiguration(EndpointConfig.get());
		this.limiters = Caffeine.newBuilder().expireAfterAccess(expirationInMs, TimeUnit.MILLISECONDS).build();
		EndpointConfig.addListener(this);
	}

	@Override
	public void onConfigChange(Config config) {
		long expirationInMs = setupConfiguration(config);
		limiters.policy().expireAfterAccess()
				.ifPresent(expiration -> expiration.setExpiresAfter(expirationInMs, TimeUnit.MILLISECONDS));
		limiters.invalidateAll();
	}

	public ThroughputLimiter get(MailboxConnection mailbox) {
		return Optional.ofNullable(mailbox) //
				.map(mb -> limiters.get(mb.logId(), this::create)) //
				.orElseGet(NoopThroughputLimiter::new);
	}

	private long setupConfiguration(Config config) {
		this.strategy = strategy(config);
		this.bypass = bypass(config);
		logger.info("[throughput-limiter] strategy:{}, initial-capacity:{}, bypass:{}", strategy, initialCapacity,
				bypass);
		return expirationInMs(config);
	}

	private static Strategy strategy(Config config) {
		try {
			return Strategy.valueOf(config.getString(ImapConfig.Throughput.STRATEGY).toUpperCase());
		} catch (IllegalArgumentException e) {
			return Strategy.BEHIND;
		}
	}

	private static Set<String> bypass(Config config) {
		try {
			return Set.copyOf(EndpointConfig.get().getStringList(ImapConfig.Throughput.BYPASS));
		} catch (Exception e) {
			logger.error("[throughput-limiter] incorrect imap.throughput.bypass value, skipping bypass: {}",
					e.getMessage());
			return Collections.emptySet();
		}
	}

	private long expirationInMs(Config config) {
		long capacity = config.getBytes(ImapConfig.Throughput.CAPACITY);
		Duration duration = config.getDuration(ImapConfig.Throughput.PERIOD);
		// Worst case scenario, we received 2 commands with literal of max size
		return 2 * (long) (initialCapacity * ((double) duration.toMillis() / capacity));
	}

	private ThroughputLimiter create(String name) {
		if (bypass.contains(name)) {
			return new NoopThroughputLimiter();
		}

		switch (strategy) {
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
		instance = null;
	}

}
