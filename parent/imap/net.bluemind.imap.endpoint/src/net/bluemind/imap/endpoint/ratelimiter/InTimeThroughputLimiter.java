package net.bluemind.imap.endpoint.ratelimiter;

public class InTimeThroughputLimiter extends BucketThroughputLimiter {

	public InTimeThroughputLimiter(int initialCapacity) {
		super(initialCapacity, true);
	}
}
