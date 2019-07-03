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
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.streams.ReadStream;

import com.ning.http.client.Body;
import com.ning.http.client.BodyGenerator;

public class ReadStreamBodyGenerator implements BodyGenerator {
	private ReadStream<?> stream;

	private final static byte[] END_PADDING = "\r\n".getBytes();
	private final static byte[] ZERO = "0".getBytes();
	private ConcurrentLinkedDeque<Buffer> queue = new ConcurrentLinkedDeque<>();
	private boolean ended;

	/** Main lock guarding all access */
	final ReentrantLock lock = new ReentrantLock();

	/** Condition for waiting takes */
	private final Condition cond = lock.newCondition();

	public ReadStreamBodyGenerator(ReadStream<?> stream2) {
		this.stream = stream2;
	}

	public void start() {
		stream.endHandler(new Handler<Void>() {

			@Override
			public void handle(Void event) {
				ended = true;
				try {
					lock.lockInterruptibly();
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
				try {
					cond.signal();

				} finally {
					lock.unlock();
				}
			}
		});

		stream.dataHandler(new Handler<Buffer>() {

			@Override
			public void handle(Buffer event) {

				stream.pause();
				try {
					lock.lockInterruptibly();
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
				try {
					queue.add(event);
					cond.signal();

				} finally {
					lock.unlock();
				}

			}

		});
	}

	@Override
	public Body createBody() throws IOException {
		return new Body() {
			private boolean eof;
			private int endDataCount;

			@Override
			public void close() throws IOException {

			}

			@Override
			public long getContentLength() {
				return -1;
			}

			@Override
			public long read(ByteBuffer buffer) throws IOException {
				Buffer qB = null;
				try {
					lock.lockInterruptibly();
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
				try {
					if (!ended && queue.isEmpty()) {
						cond.await();
					}

					qB = queue.poll();

				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				} finally {
					lock.unlock();
				}

				return readBugNetty(qB, buffer);

			}

			@SuppressWarnings("unused")
			protected long readOk(Buffer qB, ByteBuffer buffer) {
				if (qB == null) {
					return -1;
				}
				byte[] content = qB.getBytes();

				buffer.put(content);
				stream.resume();
				return buffer.position();
			}

			protected long readBugNetty(Buffer qB, ByteBuffer buffer) throws IOException {

				if (qB == null) {
					// Since we are chuncked, we must output extra bytes before
					// considering the input stream closed.
					// chunking requires to end the chunking:
					// - A Terminating chunk of "0\r\n".getBytes(),
					// - Then a separate packet of "\r\n".getBytes()
					if (!eof) {
						endDataCount++;
						if (endDataCount == 2)
							eof = true;

						if (endDataCount == 1)
							buffer.put(ZERO);

						buffer.put(END_PADDING);

						return buffer.position();
					}
					return -1;
				}
				byte[] content = qB.getBytes();

				/**
				 * Netty 3.2.3 doesn't support chunking encoding properly, so we
				 * chunk encoding ourself.
				 */

				buffer.put(Integer.toHexString(content.length).getBytes());
				// Chunking is separated by "<bytesreads>\r\n"
				buffer.put(END_PADDING);
				buffer.put(content);
				// Was missing the final chunk \r\n.
				buffer.put(END_PADDING);
				stream.resume();
				return buffer.position();
			}

		};
	}
}
