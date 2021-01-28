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

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;
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
		final ReadStream<Buffer> reader = VertxStream.read(stream);
		final AccumulatorStream writer = new AccumulatorStream();

		if (stream == null) {
			throw new RuntimeException("no stream!");
		}
		reader.pipeTo(writer, ar -> latch.countDown());
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

				b.queue(Buffer.buffer("" + count));

				if (count >= 9) {
					vertx.cancelTimer(event);
					b.end();
				}
			}

		});
		return VertxStream.stream(b);
	}

	@Override
	public Stream inContentType(String mime, String cs, String name) {
		final QueueReadStream b = new QueueReadStream();

		vertx.setPeriodic(10, new Handler<Long>() {
			private int count = 0;

			@Override
			public void handle(Long event) {
				count++;

				b.queue(Buffer.buffer("" + count));

				if (count >= 9) {
					vertx.cancelTimer(event);
					b.end();
				}
			}

		});
		return VertxStream.stream(b, mime, cs, name);
	}

	@Override
	public Stream inout(Stream stream) {

		final QueueReadStream q = new QueueReadStream();
		final ReadStream<Buffer> rStream = VertxStream.read(stream);
		rStream.endHandler(v -> q.end());
		rStream.handler(q::queue);
		rStream.resume();

		return VertxStream.stream(q);
	}

	@Override
	public String notTimeout(Stream stream) {

		final CountDownLatch latch = new CountDownLatch(1);
		final ReadStream<Buffer> reader = VertxStream.read(stream);
		final AccumulatorStream writer = new AccumulatorStream();

		if (stream == null) {
			throw new RuntimeException("no stream!");
		}
		reader.pipeTo(writer, ar -> latch.countDown());
		reader.resume();
		try {
			latch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return writer.buffer().toString();
	}
}
