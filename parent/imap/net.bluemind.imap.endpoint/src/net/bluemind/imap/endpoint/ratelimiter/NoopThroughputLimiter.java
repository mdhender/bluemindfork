package net.bluemind.imap.endpoint.ratelimiter;

import java.util.concurrent.CompletableFuture;

import net.bluemind.imap.endpoint.ImapContext;

public class NoopThroughputLimiter implements ThroughputLimiter {

	public class NoopStorage implements RateLimiterStorage {

		@Override
		public long capacity() {
			return 0l;
		}

		@Override
		public long initialCapacity() {
			return Long.MAX_VALUE;
		}

		@Override
		public long reserve(long quantity) {
			return 1_000;
		}

	}

	private final RateLimiterStorage rateLimiter;

	public NoopThroughputLimiter() {
		this.rateLimiter = new NoopStorage();
	}

	@Override
	public CompletableFuture<LimiterResult> limit(ImapContext ctx, long size) {
		return CompletableFuture.completedFuture(new LimiterResult(LimiterStatus.OK, System.currentTimeMillis()));
	}

	@Override
	public RateLimiterStorage storage() {
		return rateLimiter;
	}

	@Override
	public boolean inTime() {
		return false;
	}

}
