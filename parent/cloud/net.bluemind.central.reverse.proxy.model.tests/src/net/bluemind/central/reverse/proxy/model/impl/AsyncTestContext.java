package net.bluemind.central.reverse.proxy.model.impl;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Future;
import io.vertx.core.Promise;

public class AsyncTestContext {

	final CountDownLatch latch = new CountDownLatch(1);
	final Promise<Void> promise = Promise.promise();

	public static void asyncTest(Consumer<AsyncTestContext> test) {
		Logger logger = LoggerFactory.getLogger(AsyncTestContext.class);
		AsyncTestContext context = new AsyncTestContext();
		test.accept(context);

		try {
			boolean succeed = context.latch.await(30l, TimeUnit.SECONDS);
			assertTrue(succeed);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		if (context.future().failed()) {
			if (context.future().cause() instanceof RuntimeException) {
				throw (RuntimeException) context.future().cause();
			} else if (context.future().cause() instanceof Error) {
				throw (Error) context.future().cause();
			} else {
				throw new RuntimeException(context.future().cause());
			}

		}
	}

	public void failed(Throwable t) {
		promise.fail(t);
		latch.countDown();
	}

	public void succeeded() {
		promise.complete();
		latch.countDown();
	}

	public Future<Void> future() {
		return promise.future();
	}

	public void sleep(long duration, TimeUnit unit) {
		try {
			Thread.sleep(unit.toMillis(duration));
		} catch (InterruptedException e1) {
			Thread.currentThread().interrupt();
		}
	}

	public void assertions(Runnable assertions) {
		try {
			assertions.run();
			succeeded();
		} catch (Throwable t) {
			failed(t);
		}
	}
}
