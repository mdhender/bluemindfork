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
package net.bluemind.eas.wbxml.builder.tests;

import java.io.IOException;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;
import net.bluemind.eas.wbxml.WbxmlOutput;

public class VertxTestOutput extends WbxmlOutput {

	private static final long THRESHOLD = 256;
	private long count;
	private Buffer pending;
	private long total;
	private WriteStream<Buffer> stream;

	public VertxTestOutput(WriteStream<Buffer> stream) {
		this.stream = stream;
		this.pending = Buffer.buffer();
	}

	@Override
	public void write(int b) throws IOException {
		pending.appendByte((byte) b);
		count += 1;
		total += 1;
		flushIfNecessary(null);
	}

	private void flushIfNecessary(final QueueDrained drained) {
		if (count > THRESHOLD) {
			stream.write(pending);
			pending = Buffer.buffer();
			count = 0;
			if (drained != null) {
				if (stream.writeQueueFull()) {
					System.err.println(streamId() + ": GOT QUEUE FULL condition");
					stream.drainHandler(handler -> {
						stream.drainHandler(null);
						System.err.println(streamId() + ": DRAIN condition");
						drained.drained();
					});
				} else {
					drained.drained();
				}
			}
		} else if (drained != null) {
			drained.drained();
		}
	}

	@Override
	public void write(byte[] data) throws IOException {
		int len = data.length;
		count += len;
		total += len;
		pending.appendBytes(data, 0, len);
		flushIfNecessary(null);
	}

	@Override
	public void write(byte[] data, QueueDrained drained) {
		int len = data.length;
		count += len;
		total += len;
		pending.appendBytes(data, 0, len);
		flushIfNecessary(drained);
	}

	@Override
	public String end() {
		stream.write(pending);
		return "OK";
	}

}
