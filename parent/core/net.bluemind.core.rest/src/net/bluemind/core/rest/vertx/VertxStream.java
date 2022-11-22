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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.google.common.base.MoreObjects;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;
import net.bluemind.core.api.Stream;

public class VertxStream {

	private VertxStream() {
	}

	public static class ReadStreamStream implements Stream, ReadStream<Buffer> {

		private final ReadStream<Buffer> stream;
		private final Optional<String> mime;
		private final Optional<String> charset;
		private final Optional<String> fileName;
		private final Handler<Void> endHook;

		public ReadStreamStream(ReadStream<Buffer> delegate) {
			this(delegate, Optional.empty(), Optional.empty(), Optional.empty(), v -> {
			});
		}

		public ReadStreamStream(ReadStream<Buffer> delegate, Handler<Void> endHook) {
			this(delegate, Optional.empty(), Optional.empty(), Optional.empty(), endHook);
		}

		public ReadStreamStream(ReadStream<Buffer> delegate, Optional<String> mime, Optional<String> charset,
				Optional<String> filename, Handler<Void> endHook) {
			this.stream = delegate;
			this.mime = mime;
			this.charset = charset;
			this.fileName = filename;
			this.endHook = endHook;
		}

		@Override
		public Optional<String> charset() {
			return charset;
		}

		@Override
		public Optional<String> mime() {
			return mime;
		}

		@Override
		public Optional<String> fileName() {
			return fileName;
		}

		@Override
		public ReadStreamStream endHandler(Handler<Void> endHandler) {
			stream.endHandler(v -> {
				if (endHandler != null) {
					endHandler.handle(v);
				}
				if (endHook != null) {
					endHook.handle(v);
				}
			});
			return this;
		}

		@Override
		public ReadStreamStream handler(Handler<Buffer> handler) {
			stream.handler(handler);
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

		@Override
		public ReadStream<Buffer> fetch(long amount) {
			return this;
		}

		@Override
		public String toString() {
			return MoreObjects.toStringHelper(ReadStreamStream.class).add("wrapped", stream).toString();
		}

	}

	public static Stream stream(ReadStream<Buffer> stream) {
		if (stream instanceof Stream) {
			return (Stream) stream;
		} else {
			return wrap(stream, null, null, null, v -> {
			});
		}
	}

	public static Stream stream(ReadStream<Buffer> stream, Handler<Void> hook) {
		if (stream instanceof Stream) {
			return (Stream) stream;
		} else {
			return wrap(stream, null, null, null, hook);
		}
	}

	public static Stream stream(ReadStream<Buffer> stream, String mime) {
		return stream(stream, mime, null, null);
	}

	public static Stream stream(ReadStream<Buffer> stream, String mime, String charset) {
		return stream(stream, mime, charset, null);
	}

	public static Stream stream(ReadStream<Buffer> stream, String mime, String charset, String fileName) {
		if (stream instanceof Stream) {
			return (Stream) stream;
		} else {
			return wrap(stream, mime, charset, fileName, v -> {

			});
		}
	}

	private static Stream wrap(ReadStream<Buffer> stream, String mime, String charset, String fileName,
			Handler<Void> hook) {
		return new ReadStreamStream(stream, Optional.ofNullable(mime), Optional.ofNullable(charset),
				Optional.ofNullable(fileName), hook);
	}

	public static <T> ReadStream<T> read(Stream stream) {
		return (ReadStream<T>) stream;
	}

	public static CompletableFuture<Void> sink(Stream stream) {
		if (stream instanceof LocalPathStream) {
			return CompletableFuture.completedFuture(null);
		} else {
			ReadStream<?> vxStream = (ReadStream<?>) stream;
			vxStream.pause();
			CompletableFuture<Void> ret = new CompletableFuture<>();
			vxStream.pause();
			vxStream.handler(b -> {
			});
			vxStream.endHandler(v -> ret.complete(null));
			vxStream.exceptionHandler(ret::completeExceptionally);
			vxStream.resume();
			return ret;
		}
	}

	public static <T> ReadStream<T> readInContext(final Vertx vertx, Stream stream) {
		ReadStream<T> rs = (ReadStream<T>) stream;
		return new ReadStream<T>() {

			@Override
			public ReadStream<T> handler(Handler<T> handler) {
				vertx.runOnContext(v -> rs.handler(handler));
				return this;
			}

			@Override
			public ReadStream<T> pause() {
				vertx.runOnContext(v -> rs.pause());
				return this;
			}

			@Override
			public ReadStream<T> resume() {
				vertx.runOnContext(v -> rs.resume());
				return this;
			}

			@Override
			public ReadStream<T> exceptionHandler(Handler<Throwable> handler) {
				rs.exceptionHandler(handler);
				return this;
			}

			@Override
			public ReadStream<T> endHandler(Handler<Void> endHandler) {
				rs.endHandler(endHandler);
				return this;
			}

			@Override
			public ReadStream<T> fetch(long amount) {
				return this;
			}
		};
	}

	public static class LocalPathStream implements Stream, ReadStream<Buffer> {

		private final Path path;

		public LocalPathStream(Path p) {
			this.path = p;
		}

		public Path path() {
			return path;
		}

		@Override
		public LocalPathStream handler(Handler<Buffer> handler) {
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

		@Override
		public LocalPathStream fetch(long amount) {
			return this;
		}

	}

	public static Stream localPath(Path p) {
		return new LocalPathStream(p);
	}

	public static Stream stream(Buffer body, String mime, String charset) {
		return stream(new BufferReadStream(body), mime, charset, null);
	}

	public static Stream stream(Buffer body, String mime, String charset, String fileName) {
		return stream(new BufferReadStream(body), mime, charset, fileName);
	}

	public static Stream stream(Buffer body) {
		return stream(body, null, null);
	}

}
