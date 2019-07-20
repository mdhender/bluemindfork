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
package net.bluemind.core.rest.vertx;

import java.nio.file.Path;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.streams.ReadStream;

import net.bluemind.core.api.Stream;

public class VertxStream {

	private VertxStream() {
	}

	public static class ReadStreamStream implements Stream, ReadStream<ReadStreamStream> {

		private final ReadStream<?> stream;

		public ReadStreamStream(ReadStream<?> delegate) {
			this.stream = delegate;
		}

		@Override
		public ReadStreamStream endHandler(Handler<Void> endHandler) {
			stream.endHandler(endHandler);
			return this;
		}

		@Override
		public ReadStreamStream dataHandler(Handler<Buffer> handler) {
			stream.dataHandler(handler);
			return this;
		}

		@Override
		public ReadStreamStream pause() {
			stream.pause();
			return this;
		}

		@Override
		public ReadStreamStream resume() {
			stream.resume();
			return this;
		}

		@Override
		public ReadStreamStream exceptionHandler(Handler<Throwable> handler) {
			stream.exceptionHandler(handler);
			return this;
		}

	}

	public static <T> Stream stream(ReadStream<T> stream) {
		if (stream instanceof Stream) {
			return (Stream) stream;
		} else {
			return wrap(stream);
		}
	}

	private static <T> Stream wrap(ReadStream<?> stream) {
		return new ReadStreamStream(stream);
	}

	public static ReadStream<?> read(Stream stream) {
		return (ReadStream<?>) stream;
	}

	public static ReadStream<?> readInContext(final Vertx vertx, Stream stream) {
		ReadStream<?> rs = (ReadStream<?>) stream;
		return new ReadStream<Void>() {

			@Override
			public Void dataHandler(Handler<Buffer> handler) {
				vertx.runOnContext((Void) -> rs.dataHandler(handler));
				return null;
			}

			@Override
			public Void pause() {
				vertx.runOnContext((Void) -> rs.pause());
				return null;
			}

			@Override
			public Void resume() {
				vertx.runOnContext((Void) -> rs.resume());
				return null;
			}

			@Override
			public Void exceptionHandler(Handler<Throwable> handler) {
				rs.exceptionHandler(handler);
				return null;
			}

			@Override
			public Void endHandler(Handler<Void> endHandler) {
				rs.endHandler(endHandler);
				return null;
			}
		};
	}

	public static class LocalPathStream implements Stream, ReadStream<LocalPathStream> {

		private final Path path;

		public LocalPathStream(Path p) {
			this.path = p;
		}

		public Path path() {
			return path;
		}

		@Override
		public LocalPathStream dataHandler(Handler<Buffer> handler) {
			return this;
		}

		@Override
		public LocalPathStream pause() {
			return this;
		}

		@Override
		public LocalPathStream resume() {
			return this;
		}

		@Override
		public LocalPathStream exceptionHandler(Handler<Throwable> handler) {
			return this;
		}

		@Override
		public LocalPathStream endHandler(Handler<Void> endHandler) {
			return this;
		}

	}

	public static Stream localPath(Path p) {
		return new LocalPathStream(p);
	}

	public static Stream stream(Buffer body) {
		return stream(new BufferReadStream(body));
	}

}
