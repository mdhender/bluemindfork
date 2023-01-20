package net.bluemind.imap.endpoint.ratelimiter;

public class BehindThroughputLimiter extends BucketThroughputLimiter {

	public BehindThroughputLimiter(int initialCapacity) {
		super(initialCapacity, false);
	}

}
