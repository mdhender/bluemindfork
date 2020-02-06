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
package net.bluemind.filehosting.filesystem.service.internal;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;

public class StreamThread extends Thread {
	public Handler<Buffer> dh;
	public Handler<Void> eh;
	public Handler<Throwable> ex;
	private final InputStream in;
	public boolean paused;
	private final List<byte[]> buffer;
	private final int bufferSize = 1024 * 64;

	Logger logger = LoggerFactory.getLogger(StreamThread.class);

	public StreamThread(InputStream in) {
		this.in = new BufferedInputStream(in);
		this.buffer = new ArrayList<>();
	}

	@Override
	public void run() {
		try {
			while (true) {
				byte[] streambuffer = new byte[bufferSize];
				int read = in.read(streambuffer, 0, bufferSize);
				if (read == -1) {
					break;
				}
				byte readBytes[] = new byte[read];
				System.arraycopy(streambuffer, 0, readBytes, 0, read);
				buffer.add(readBytes);
				tryWrite();
			}
			while (buffer.size() > 0) {
				tryWrite();
			}
			if (null != eh) {
				eh.handle(null);
			}
			in.close();
		} catch (Exception e) {
			logger.warn("Serving file from node failed", e);
		}

	}

	private void tryWrite() throws InterruptedException {
		if (!paused && !buffer.isEmpty()) {
			byte[] bs = buffer.get(0);
			dh.handle(Buffer.buffer(bs));
			buffer.remove(0);
		} else {
			Thread.sleep(10);
		}
	}

}