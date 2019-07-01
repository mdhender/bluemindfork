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

import java.util.concurrent.CountDownLatch;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.streams.Pump;
import org.vertx.java.core.streams.ReadStream;

import net.bluemind.core.api.Stream;
import net.bluemind.core.rest.vertx.VertxStream;

public class RestStreamImpl implements IRestStreamTestService {

	private Vertx vertx;

	public RestStreamImpl(Vertx vertx) {
		this.vertx = vertx;
	}

	@Override
	public String out(Stream stream) {

		final CountDownLatch latch = new CountDownLatch(1);
		final ReadStream<?> reader = VertxStream.read(stream);
		final AccumulatorStream writer = new AccumulatorStream();

		if (stream == null) {
			throw new RuntimeException("no stream!");
		}
		reader.endHandler(new Handler<Void>() {

			@Override
			public void handle(Void event) {
				latch.countDown();
			}
		});

		Pump pump = Pump.createPump(reader, writer);
		pump.start();
		reader.resume();
		try {
			latch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return writer.buffer().toString();

	}

	@Override
	public Stream in() {
		final QueueReadStream b = new QueueReadStream();

		vertx.setPeriodic(10, new Handler<Long>() {
			private int count = 0;

			@Override
			public void handle(Long event) {
				count++;

				b.queue(new Buffer("" + count));

				if (count >= 9) {
					vertx.cancelTimer(event);
					b.end();
				}
			}

		});
		return VertxStream.stream(b);
	}

	@Override
	public Stream inout(Stream stream) {

		final QueueReadStream q = new QueueReadStream();
		final ReadStream<?> rStream = VertxStream.readInContext(vertx, stream);
		rStream.endHandler(new Handler<Void>() {

			@Override
			public void handle(Void event) {
				q.end();
			}
		});

		rStream.dataHandler(new Handler<Buffer>() {

			@Override
			public void handle(Buffer event) {
				q.queue(event);
			}
		});
		rStream.resume();

		return VertxStream.stream(q);
	}

	@Override
	public String notTimeout(Stream stream) {

		final CountDownLatch latch = new CountDownLatch(1);
		final ReadStream<?> reader = VertxStream.read(stream);
		final AccumulatorStream writer = new AccumulatorStream();

		if (stream == null) {
			throw new RuntimeException("no stream!");
		}
		reader.endHandler(new Handler<Void>() {

			@Override
			public void handle(Void event) {
				latch.countDown();
			}
		});

		Pump pump = Pump.createPump(reader, writer);
		pump.start();
		reader.resume();
		try {
			latch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return writer.buffer().toString();
	}
}
