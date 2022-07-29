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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;
import net.bluemind.core.api.Stream;
import net.bluemind.core.rest.base.GenericStream;
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

	@Test
	public void testFlakyInOut() throws Exception {
		int ok = 0;
		for (int i = 0; i < 500; i++) {
			System.err.println("RUN " + i);
			try {
				testInOut();
				ok++;
			} catch (AssertionError e) {
				System.err.println("ERROR after " + ok + " loops.");
				throw e;
			}
		}
	}

	private static class GenericReadStream implements ReadStream<Buffer> {

		private Handler<Buffer> dataHandler;
		private Handler<Void> endHandler;
		private boolean paused;
		private byte[] content;
		private Queue<Buffer> toStream = new LinkedBlockingDeque<>();

		public GenericReadStream(int chunks, int packetSize) {
			byte[] b = new byte[packetSize];
			ThreadLocalRandom rd = ThreadLocalRandom.current();
			Buffer total = Buffer.buffer();
			for (int i = 0; i < chunks; i++) {
				for (int j = 0; j < packetSize; j++) {
					b[j] = (byte) rd.nextInt((int) 'a', (int) 'z');
				}
				total.appendBytes(b);
				toStream.add(Buffer.buffer(b));
			}
			content = total.getBytes();
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

			while (!paused && !toStream.isEmpty()) {
				dataHandler.handle(toStream.poll());
			}

			if (toStream.isEmpty() && endHandler != null) {
				endHandler.handle(null);
				endHandler = null;
			}

		}

		public byte[] content() {
			return content;
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
			}
			next();
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
		GenericReadStream stream = new GenericReadStream(packetCount, packetSize);
		final AccumulatorStream accu = new AccumulatorStream();
		final CountDownLatch latch = new CountDownLatch(1);

		long time = System.nanoTime();
		Stream out = service.inout(VertxStream.stream(stream));

		final ReadStream<Buffer> readStream = VertxStream.read(out);

		readStream.pipeTo(accu, ar -> latch.countDown());

		assertTrue(latch.await(4, TimeUnit.SECONDS));
		long f = System.nanoTime();
		System.out
				.println("time to transfert " + stream.content.toString().length() + " " + ((f - time) / (1000 * 1000))
						+ " time per packet " + (((double) (f - time)) / (1000.0 * 1000.0 * packetCount)));

		assertEquals(new String(stream.content()), accu.buffer().toString());

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

		GenericReadStream stream = new GenericReadStream(5000, 1);

		String ret = service.out(VertxStream.stream(stream));
		assertEquals(new String(stream.content()), ret);
	}

	@Test
	public void testInOut() throws Exception {
		IRestStreamTestService service = getService();

		GenericReadStream stream = new GenericReadStream(20000, 1024);
		Stream input = VertxStream.stream(stream);

		Stream streamOut = service.inout(input);

		Buffer out = GenericStream.asyncStreamToBuffer(streamOut).get(15, TimeUnit.SECONDS);
		System.err.println("accu.buffer.length: " + out.length());
		assertArrayEquals(stream.content(), out.getBytes());
	}

	@Test
	public void testIn() throws InterruptedException {
		final IRestStreamTestService service = getService();
		final AccumulatorStream accu = new AccumulatorStream();

		final CountDownLatch latch = new CountDownLatch(1);

		Stream in = service.in();

		final ReadStream<Buffer> readStream = VertxStream.read(in);

		readStream.pipeTo(accu, h -> latch.countDown());
		latch.await(60, TimeUnit.SECONDS);

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
