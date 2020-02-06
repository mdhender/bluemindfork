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
package net.bluemind.core.rest.http;

import java.io.IOException;
import java.io.InputStream;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;

public class ReadStreamInputStream extends InputStream {
	private boolean ended;
	private ReadStream<Buffer> stream;

	private Buffer currentBuffer;
	private int pos;

	public ReadStreamInputStream(ReadStream<Buffer> stream2) {
		this.stream = stream2;
	}

	public void start() {
		stream.endHandler(new Handler<Void>() {

			@Override
			public void handle(Void event) {
				ended = true;
			}
		});

		stream.handler(new Handler<Buffer>() {

			@Override
			public void handle(Buffer event) {
				ReadStreamInputStream.this.currentBuffer = event;
				stream.pause();
			}

		});
	}

	@Override
	public int read() throws IOException {
		if (currentBuffer == null || (ended && currentBuffer.length() < (pos + 1))) {
			return -1;
		}

		if (currentBuffer.length() < (pos + 1)) {
			stream.resume();
		}
		byte ret = currentBuffer.getByte(pos);
		pos++;
		return ret;
	}
}
