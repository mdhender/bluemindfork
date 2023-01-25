package net.bluemind.imap.endpoint.ratelimiter;

import static net.bluemind.imap.endpoint.ImapMetricsHolder.BufferStatus.ALLOWED;
import static net.bluemind.imap.endpoint.ImapMetricsHolder.BufferStatus.LIMITED;
import static net.bluemind.imap.endpoint.ImapMetricsHolder.BufferStatus.OVERFLOW;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.imap.endpoint.ImapContext;
import net.bluemind.imap.endpoint.ImapMetricsHolder.BufferStatus;

public interface ThroughputLimiter {
	static final Logger logger = LoggerFactory.getLogger(ThroughputLimiter.class);

	public static record LimiterResult(BufferStatus status, long size, long quantityLimited, long refillTime) {

		public static LimiterResult literalOverflow(long size) {
			return new LimiterResult(BufferStatus.OVERFLOW, size, 0, 0);
		}
	}

	public interface RateLimiterStorage {
		long capacity();

		long initialCapacity();

		/**
		 * Reserve the given quantity and return the number of nanoseconds to wait until
		 * its available.
		 * 
		 * @param quantity
		 * @return the number of nanoseconds to wait until quantity is available.
		 */
		long reserve(long quantity);
	}

	RateLimiterStorage storage();

	default CompletableFuture<LimiterResult> limit(ImapContext ctx, long size) {
		return limit(System.currentTimeMillis(), ctx, size);
	}

	default CompletableFuture<LimiterResult> limit(long startedAt, ImapContext ctx, long size) {
		long capacityBeforeReservation = storage().capacity();
		long toReserve = Math.min(storage().initialCapacity(), size);
		long limited = Math.max(0, size - capacityBeforeReservation);
		long whenAvailableInNanoseconds = storage().reserve(toReserve);

		CompletableFuture<LimiterResult> future = new CompletableFuture<>();
		if (whenAvailableInNanoseconds < 1_000_000) {
			future.complete(new LimiterResult(ALLOWED, size, limited, 0));
		} else {
			BufferStatus status = toReserve != size ? OVERFLOW : LIMITED;
			Duration refilledIn = Duration.ofNanos(whenAvailableInNanoseconds);
			LimiterResult futureResult = new LimiterResult(status, size, limited, refilledIn.toMillis());
			ctx.socket().pause();
			ctx.vertx().setTimer(refilledIn.toMillis(), timerId -> {
				ctx.socket().resume();
				if (inTime()) {
					future.complete(futureResult);
				}
			});
			if (!inTime()) {
				future.complete(futureResult);
			}
		}

		return future.thenApply(result -> {
			if (logger.isDebugEnabled()) {
				logger.debug("[throughput-limiter][{}] done:{} in {}ms", ctx.mailbox().login(), result.status(),
						result.refillTime);
			}
			return result;
		});
	}

	boolean inTime();

}
