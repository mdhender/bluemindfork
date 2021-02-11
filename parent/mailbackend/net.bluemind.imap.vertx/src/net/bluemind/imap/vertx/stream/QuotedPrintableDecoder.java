/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.imap.vertx.stream;

import io.netty.util.ByteProcessor;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;
import net.bluemind.lib.vertx.Result;

public class QuotedPrintableDecoder implements WriteStream<Buffer> {

	private final WriteStream<Buffer> delegate;
	private QPProc qpProc;

	public QuotedPrintableDecoder(WriteStream<Buffer> delegate) {
		this.delegate = delegate;
		this.qpProc = new QPProc();
	}

	private enum State {
		normal, first_byte_or_cr, lf, second_byte;
	}

	private static class QPProc {
		State st = State.normal;
		private Buffer output;
		private int num;

		public QPProc() {
			this.output = Buffer.buffer();
		}

		public Buffer flush() {
			Buffer ret = output;
			this.output = Buffer.buffer();
			return ret;

		}

		public void handle(byte b) {
			switch (st) {
			case normal:
				if (b == '=') {
					st = State.first_byte_or_cr;
				} else {
					output.appendByte(b);
				}
				break;
			case first_byte_or_cr:
				if (b == '\r') {
					st = State.lf;
				} else {
					num = 16 * num(b);
					st = State.second_byte;
				}
				break;
			case lf:
				st = State.normal;
				break;
			case second_byte:
				num += num(b);
				output.appendByte((byte) (num & 0xFF));
				st = State.normal;
				break;
			}
		}

		private int num(byte b) {
			switch (b) {
			case '0':
				return 0;
			case '1':
				return 1;
			case '2':
				return 2;
			case '3':
				return 3;
			case '4':
				return 4;
			case '5':
				return 5;
			case '6':
				return 6;
			case '7':
				return 7;
			case '8':
				return 8;
			case '9':
				return 9;
			case 'A':
				return 10;
			case 'B':
				return 11;
			case 'C':
				return 12;
			case 'D':
				return 13;
			case 'E':
				return 14;
			case 'F':
				return 15;
			default:
				throw new RuntimeException("unexpected byte " + b);
			}
		}

	}

	@Override
	public QuotedPrintableDecoder exceptionHandler(Handler<Throwable> handler) {
		return this;
	}

	@Override
	public QuotedPrintableDecoder write(Buffer data) {
		data.getByteBuf().forEachByte(new ByteProcessor() {

			@Override
			public boolean process(byte value) throws Exception {
				qpProc.handle(value);
				return true;
			}

		});
		delegate.write(qpProc.flush());

		return this;
	}

	@Override
	public QuotedPrintableDecoder write(Buffer data, Handler<AsyncResult<Void>> handler) {
		write(data);
		handler.handle(Result.success());
		return this;
	}

	@Override
	public void end() {
		delegate.end();
	}

	@Override
	public void end(Handler<AsyncResult<Void>> handler) {
		end();
		handler.handle(Result.success());
	}

	@Override
	public QuotedPrintableDecoder setWriteQueueMaxSize(int maxSize) {
		return this;
	}

	@Override
	public boolean writeQueueFull() {
		return delegate.writeQueueFull();
	}

	@Override
	public QuotedPrintableDecoder drainHandler(Handler<Void> handler) {
		delegate.drainHandler(handler);
		return this;
	}

}
