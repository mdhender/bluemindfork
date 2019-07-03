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
package net.bluemind.core.rest.http.internal;

import static java.nio.charset.StandardCharsets.US_ASCII;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.streams.Pump;
import org.vertx.java.core.streams.ReadStream;
import org.vertx.java.core.streams.WriteStream;

import com.ning.http.client.Body;
import com.ning.http.client.BodyGenerator;

public class BodyGeneratorStream extends FeedableBodyGenerator
		implements BodyGenerator, WriteStream<BodyGeneratorStream> {

	private static final Logger logger = LoggerFactory.getLogger(BodyGeneratorStream.class);
	private ReadStream<?> bodyStream;
	private Handler<Void> drainHandler;
	private final static byte[] ZERO = "".getBytes(US_ASCII);

	public BodyGeneratorStream(ReadStream<?> bodyStream) {
		this.bodyStream = bodyStream;
	}

	@Override
	public Body createBody() throws IOException {
		bodyStream.endHandler(new Handler<Void>() {

			@Override
			public void handle(Void event) {
				logger.debug("send end of stream");
				try {
					feed(ByteBuffer.wrap(ZERO), true);
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
		});
		Pump.createPump(bodyStream, this).start();
		return super.createBody();
	}

	@Override
	public BodyGeneratorStream exceptionHandler(Handler<Throwable> handler) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BodyGeneratorStream setWriteQueueMaxSize(int maxSize) {
		return this;
	}

	@Override
	public boolean writeQueueFull() {
		return false;
	}

	@Override
	public BodyGeneratorStream drainHandler(Handler<Void> handler) {
		this.drainHandler = handler;
		return this;
	}

	@Override
	public BodyGeneratorStream write(Buffer data) {
		logger.debug("send chunck of data {}", data);
		try {
			feed(ByteBuffer.wrap(data.getBytes()), false);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return this;
	}

}
