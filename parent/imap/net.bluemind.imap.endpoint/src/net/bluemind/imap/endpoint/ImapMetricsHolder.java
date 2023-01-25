package net.bluemind.imap.endpoint;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.spectator.api.Counter;
import com.netflix.spectator.api.Registry;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import net.bluemind.imap.endpoint.ratelimiter.ThroughputLimiter.LimiterResult;
import net.bluemind.metrics.registry.IdFactory;
import net.bluemind.metrics.registry.MetricsRegistry;

public class ImapMetricsHolder {

	private static final Logger logger = LoggerFactory.getLogger(ImapMetricsHolder.class);

	private static final ImapMetricsHolder INSTANCE = new ImapMetricsHolder();

	public static ImapMetricsHolder get() {
		return INSTANCE;
	}

	public enum BufferStatus {
		ALLOWED, LIMITED, OVERFLOW
	}

	private final Registry registry = MetricsRegistry.get();
	private final IdFactory idFactory = new IdFactory("imap", registry, ImapSession.class);

	private final Map<BufferStatus, Counter> bufferStatusCounter = new EnumMap<>(BufferStatus.class);

	private final Duration limiterLogPeriod = EndpointConfig.get().getDuration("imap.throughput.log-period");

	private final Bucket logLimitatedLimiter;

	private ImapMetricsHolder() {
		Stream.of(BufferStatus.values()).forEach(status -> bufferStatusCounter.put(status,
				registry.counter(idFactory.name("bufferStatusCount", "status", status.name().toLowerCase()))));
		Refill refill = Refill.greedy(1, limiterLogPeriod);
		Bandwidth bandwidth = Bandwidth.classic(1, refill);
		this.logLimitatedLimiter = Bucket.builder().addLimit(bandwidth).build();
	}

	public void monitorBufferStatus(String name, LimiterResult limiterResult) {
		bufferStatusCounter.get(limiterResult.status()).increment();
		switch (limiterResult.status()) {
		case ALLOWED:
			logger.debug("[throughput-limiter][{}] Accepting {}b", name, limiterResult.size());
			break;
		case LIMITED:
			registry.counter(idFactory.name("usersWaitingDuration", "user", name))
					.increment(limiterResult.refillTime());
			if (logLimitatedLimiter.tryConsume(1)) {
				logger.info("[throughput-limiter][{}] Limiting {}b: refill in {}ms", name, limiterResult.size(),
						limiterResult.refillTime());
			}
			break;
		case OVERFLOW:
			logger.error("[throughput-limiter][{}] Literal size is too big ({})", name, limiterResult.size());
			break;
		}
	}
}
