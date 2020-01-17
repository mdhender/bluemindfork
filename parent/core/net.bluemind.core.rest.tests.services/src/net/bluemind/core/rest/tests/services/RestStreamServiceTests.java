/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.core.rest.tests.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.Pump;
import io.vertx.core.streams.ReadStream;
import net.bluemind.core.api.Stream;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.lib.vertx.VertxPlatform;

public class RestStreamServiceTests {

	private Vertx vertx;

	@Before
	public void before() throws Exception {
		VertxPlatform pm = new VertxPlatform();
		pm.start(null);
		vertx = VertxPlatform.getVertx();
	}

	@Test
	public void testLotOfCalls() throws InterruptedException {
		for (int i = 0; i < 10; i++) {
			testOut();
			testOut2();
			testIn();
			testReadSpeed();
		}
	}

	private static class GenericReadStream implements ReadStream<Buffer> {

		private Handler<Buffer> dataHandler;
		private Handler<Void> endHandler;
		private int count;
		private boolean ended;
		private boolean paused;
		private Buffer content = Buffer.buffer();
		private int size;
		private int packetSize;

		public GenericReadStream(int i, int packetSize) {
			this.size = i;
			this.packetSize = packetSize;
		}

		@Override
		public GenericReadStream handler(Handler<Buffer> handler) {
			this.dataHandler = handler;

			if (!paused) {
				next();
			}
			return this;
		}

		private void next() {

			while (!paused && !ended) {
				count++;
				Buffer value = produce(count);
				dataHandler.handle(value);
				content.appendBuffer(value);
				if (count > size) {
					ended = true;
				}
			}

			if (ended) {
				System.out.println("ended");
				if (endHandler != null) {
					endHandler.handle(null);
				} else {
					System.err.println("no end handler");
				}
			}

		}

		public Buffer content() {
			return content;
		}

		protected Buffer produce(int count) {
			String v = "";
			for (int i = 0; i < packetSize; i++) {
				v += (count % 9);
			}
			return Buffer.buffer(v);
		}

		@Override
		public GenericReadStream pause() {
			paused = true;
			System.out.println("paused !!");
			return this;
		}

		@Override
		public GenericReadStream resume() {
			if (paused) {
				System.out.println("resume !!");
				paused = false;
				next();
			}
			return this;
		}

		@Override
		public GenericReadStream exceptionHandler(Handler<Throwable> handler) {
			return this;
		}

		@Override
		public GenericReadStream endHandler(Handler<Void> endHandler) {
			this.endHandler = endHandler;
			return this;
		}

		@Override
		public ReadStream<Buffer> fetch(long amount) {
			return this;
		}

	}

	@Test
	public void testReadSpeed() throws InterruptedException {
		IRestStreamTestService service = getService();
		int packetCount = 10;
		int packetSize = 1;
		GenericReadStream stream = new GenericReadStream(packetCount, packetSize);// *
																					// 1000,
		// 400);
		final AccumulatorStream accu = new AccumulatorStream();
		final CountDownLatch latch = new CountDownLatch(1);

		long time = System.nanoTime();
		Stream out = service.inout(VertxStream.stream(stream));

		final ReadStream<Buffer> readStream = VertxStream.read(out);

		Handler<Void> endHandler = new Handler<Void>() {

			@Override
			public void handle(Void event) {
				latch.countDown();
			}

		};
		readStream.endHandler(endHandler);

		Pump.pump(readStream, accu).start();
		// readStream.resume();

		assertTrue(latch.await(4, TimeUnit.SECONDS));
		long f = System.nanoTime();
		System.out
				.println("time to transfert " + stream.content.toString().length() + " " + ((f - time) / (1000 * 1000))
						+ " time per packet " + (((double) (f - time)) / (1000.0 * 1000.0 * packetCount)));

		assertEquals(stream.content().toString(), accu.buffer().toString());

	}

	@Test
	public void testOut() {
		IRestStreamTestService service = getService();
		final QueueReadStream stream = new QueueReadStream();
		final StringBuilder sb = new StringBuilder();

		vertx.setPeriodic(100, new Handler<Long>() {
			private int count = 0;

			@Override
			public void handle(Long event) {
				count++;
				String v = "" + (count % 10);
				sb.append(v);
				stream.queue(Buffer.buffer(v));

				if (count > 5) {
					stream.end();
					vertx.cancelTimer(event);
				}
			}
		});

		String ret = service.out(VertxStream.stream(stream));
		assertEquals(sb.toString(), ret);
	}

	@Test
	public void testOut2() {
		IRestStreamTestService service = getService();

		final StringBuilder sb = new StringBuilder();

		GenericReadStream stream = new GenericReadStream(5000, 1) {

			@Override
			protected Buffer produce(int count) {
				Buffer v = super.produce(count);
				sb.append(v);
				return v;
			}

		};

		String ret = service.out(VertxStream.stream(stream));
		assertEquals(sb.toString(), ret);
	}

	@Test
	public void testInOut() throws Exception {
		IRestStreamTestService service = getService();

		final StringBuilder sb = new StringBuilder();

		GenericReadStream stream = new GenericReadStream(2000, 1) {

			@Override
			protected Buffer produce(int count) {
				Buffer v = super.produce(count);
				sb.append(v);
				return v;
			}

		};

		Stream streamOut = service.inout(VertxStream.stream(stream));

		final AccumulatorStream accu = new AccumulatorStream();

		final CountDownLatch latch = new CountDownLatch(1);
		final ReadStream<Buffer> readStream = VertxStream.read(streamOut);

		Handler<Void> endHandler = new Handler<Void>() {

			@Override
			public void handle(Void event) {
				latch.countDown();
			}

		};
		readStream.endHandler(endHandler);

		Pump.pump(readStream, accu).start();
		assertTrue(latch.await(5, TimeUnit.SECONDS));

		assertEquals(sb.toString(), accu.buffer().toString());
	}

	@Test
	public void testIn() throws InterruptedException {
		final IRestStreamTestService service = getService();
		final AccumulatorStream accu = new AccumulatorStream();

		final CountDownLatch latch = new CountDownLatch(1);

		Stream in = service.in();

		final ReadStream<Buffer> readStream = VertxStream.read(in);

		Handler<Void> endHandler = new Handler<Void>() {

			@Override
			public void handle(Void event) {
				latch.countDown();
			}

		};
		readStream.endHandler(endHandler);

		Pump.pump(readStream, accu).start();
		// readStream.resume();

		latch.await();

		assertEquals("123456789", accu.buffer().toString());
	}

	protected IRestStreamTestService getService() {
		return new RestStreamImpl(vertx);
	}

	@Test
	public void testTimeout() {
		IRestStreamTestService service = getService();
		final QueueReadStream stream = new QueueReadStream();
		final StringBuilder sb = new StringBuilder();

		vertx.setPeriodic(1000, new Handler<Long>() {
			private int count = 0;

			@Override
			public void handle(Long event) {
				count++;
				String v = "" + (count % 10);
				sb.append(v);
				stream.queue(Buffer.buffer(v));

				if (count > 25) {
					stream.end();
					vertx.cancelTimer(event);
				}
			}
		});

		String ret = service.out(VertxStream.stream(stream));
		assertEquals(sb.toString(), ret);
	}
}
