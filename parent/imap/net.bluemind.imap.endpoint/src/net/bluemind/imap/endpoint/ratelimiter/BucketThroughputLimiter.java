package net.bluemind.imap.endpoint.ratelimiter;

import java.time.Duration;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import net.bluemind.imap.endpoint.EndpointConfig;

public abstract class BucketThroughputLimiter implements ThroughputLimiter {

	public class Bucket4jStorage implements RateLimiterStorage {

		private final Bandwidth bandwidth;
		private final Bucket bucket;

		private final long capacity = EndpointConfig.get().getBytes("imap.throughput.capacity");
		private final Duration duration = EndpointConfig.get().getDuration("imap.throughput.period");

		public Bucket4jStorage(int initialCapacity) {
			Refill refill = Refill.greedy(capacity, duration);
			this.bandwidth = Bandwidth.classic(initialCapacity, refill);
			this.bucket = Bucket.builder().addLimit(bandwidth).build();
		}

		@Override
		public long capacity() {
			return bucket.getAvailableTokens();
		}

		@Override
		public long initialCapacity() {
			return bandwidth.getInitialTokens();
		}

		@Override
		public long reserve(long quantity) {
			return bucket.consumeIgnoringRateLimits(quantity);
		}

	}

	private final RateLimiterStorage rateLimiter;
	private final boolean inTime;

	protected BucketThroughputLimiter(int initialCapacity, boolean inTime) {
		this.rateLimiter = new Bucket4jStorage(initialCapacity);
		this.inTime = inTime;
	}

	@Override
	public RateLimiterStorage storage() {
		return rateLimiter;
	}

	@Override
	public boolean inTime() {
		return inTime;
	}

}
