package net.bluemind.imap.endpoint.ratelimiter;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.imap.endpoint.ImapContext;

public interface ThroughputLimiter {
	static final Logger logger = LoggerFactory.getLogger(ThroughputLimiter.class);

	public enum LimiterStatus {
		OK, OVERFLOW
	}

	public static record LimiterResult(LimiterStatus status, long startedAt) {

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
		String name = ctx.mailbox().login();
		long capacityBeforeReservation = storage().capacity();
		long toReserve = Math.min(storage().initialCapacity(), size);
		long whenAvailableInNanoseconds = storage().reserve(toReserve);

		LimiterStatus status = toReserve != size ? LimiterStatus.OVERFLOW : LimiterStatus.OK;
		LimiterResult futureResult = new LimiterResult(status, startedAt);

		CompletableFuture<LimiterResult> future = new CompletableFuture<>();
		if (whenAvailableInNanoseconds < 1_000_000) {
			logger.info("[throughput-limiter][{}] Accepting {}b", name, size);
			future.complete(futureResult);
		} else {
			Duration refilledIn = Duration.ofNanos(whenAvailableInNanoseconds);
			logger.info("[throughput-limiter][{}] Limiting {}b : wait for refill in {}ms (capacity before:{})", name,
					size, refilledIn.toMillis(), capacityBeforeReservation);
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
				long delay = System.currentTimeMillis() - result.startedAt();
				logger.debug("[throughput-limiter][{}] done:{} in {}ms", ctx.mailbox().login(), result.status(), delay);
			}
			return result;
		});
	}

	boolean inTime();

}
