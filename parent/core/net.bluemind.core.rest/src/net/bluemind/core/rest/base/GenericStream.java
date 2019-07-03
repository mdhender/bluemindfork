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
package net.bluemind.core.rest.base;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.streams.Pump;
import org.vertx.java.core.streams.ReadStream;
import org.vertx.java.core.streams.WriteStream;

import net.bluemind.core.api.Stream;
import net.bluemind.core.rest.vertx.VertxStream;

public abstract class GenericStream<T> implements ReadStream<GenericStream<T>> {

	private static Logger logger = LoggerFactory.getLogger(GenericStream.class);

	private Handler<Buffer> dataHandler;
	private boolean paused;
	private Handler<Throwable> exceptionHandler;
	private Handler<Void> endHandler;
	private boolean ended;

	@Override
	public GenericStream<T> dataHandler(Handler<Buffer> dataHandler) {
		this.dataHandler = dataHandler;
		read();
		return this;
	}

	@Override
	public GenericStream<T> pause() {
		this.paused = true;
		return this;
	}

	@Override
	public GenericStream<T> resume() {
		if (this.paused) {
			paused = false;
			read();
		}
		return this;
	}

	@Override
	public GenericStream<T> exceptionHandler(Handler<Throwable> exceptionHandler) {
		this.exceptionHandler = exceptionHandler;
		return this;
	}

	@Override
	public GenericStream<T> endHandler(Handler<Void> endHandler) {
		this.endHandler = endHandler;
		return this;
	}

	private void read() {
		if (paused) {
			return;
		}

		while (!ended && !paused) {
			T n;
			try {
				StreamState<T> currentState = next();
				if (currentState.state == State.ENDED) {
					ended = true;
					if (endHandler != null) {
						endHandler.handle(null);
					}
				} else {
					n = currentState.value;
					dataHandler.handle(serialize(n));
				}

			} catch (Exception e) {
				error(e);
			}

		}

	}

	protected abstract Buffer serialize(T n) throws Exception;

	protected abstract StreamState<T> next() throws Exception;

	private void error(Exception e) {
		if (exceptionHandler != null) {
			exceptionHandler.handle(e);
		}
		logger.error("error reading backup stream", e);
		ended = true;
		if (endHandler != null) {
			endHandler.handle(null);
		}
	}

	public static String streamToString(Stream stream) {
		final ReadStream<?> reader = VertxStream.read(stream);
		final AccumulatorStream writer = new AccumulatorStream();
		stream(reader, writer);
		return writer.buffer().toString();
	}

	public static CompletableFuture<Buffer> asyncStreamToBuffer(Stream stream) {
		final ReadStream<?> reader = VertxStream.read(stream);
		final AccumulatorStream writer = new AccumulatorStream();
		return asyncStream(reader, writer).thenApply(v -> writer.buffer());
	}

	public static void streamToFile(Stream stream, File file) {
		final ReadStream<?> reader = VertxStream.read(stream);
		try (FileWriterStream writer = new FileWriterStream(file)) {
			stream(reader, writer);
		}
	}

	private static void stream(final ReadStream<?> reader, final WriteStream<?> writer) {
		final CountDownLatch latch = new CountDownLatch(1);
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
			logger.error(e.getMessage(), e);
			Thread.currentThread().interrupt();
		}
	}

	private static CompletableFuture<Void> asyncStream(final ReadStream<?> reader, final WriteStream<?> writer) {
		CompletableFuture<Void> prom = new CompletableFuture<>();
		reader.endHandler(new Handler<Void>() {

			@Override
			public void handle(Void event) {
				prom.complete(event);
			}
		});

		Pump pump = Pump.createPump(reader, writer);
		pump.start();
		reader.resume();
		return prom;
	}

	private abstract static class BaseStream<T> implements WriteStream<T> {

		@Override
		public T exceptionHandler(Handler<Throwable> handler) {
			return (T) this;
		}

		@Override
		public T setWriteQueueMaxSize(int maxSize) {
			return (T) this;
		}

		@Override
		public boolean writeQueueFull() {
			return false;
		}

		@Override
		public T drainHandler(Handler<Void> handler) {
			return (T) this;
		}

		@Override
		public abstract T write(Buffer data);

	}

	private static class AccumulatorStream extends BaseStream<AccumulatorStream>
			implements WriteStream<AccumulatorStream> {

		private Buffer buffer = new Buffer();

		@Override
		public AccumulatorStream write(Buffer data) {
			synchronized (this) {
				buffer.appendBuffer(data);
			}
			return this;

		}

		public Buffer buffer() {
			return buffer;
		}
	}

	private static class FileWriterStream extends BaseStream<FileWriterStream>
			implements WriteStream<FileWriterStream>, AutoCloseable {

		private OutputStream out;
		private Logger logger = LoggerFactory.getLogger(FileWriterStream.class);

		public FileWriterStream(File file) {
			try {
				out = Files.newOutputStream(file.toPath(), StandardOpenOption.CREATE_NEW);
			} catch (IOException e) {
				logger.warn("Cannot open new file {} for writing", file.getAbsolutePath(), e);
			}
		}

		@Override
		public FileWriterStream write(Buffer data) {
			try {
				out.write(data.getBytes());
			} catch (IOException e) {
				logger.warn("Cannot stream to file", e);
			}
			return this;

		}

		@Override
		public void close() {
			try {
				out.close();
			} catch (IOException e) {
			}
		}
	}

	public static class StreamState<T> {
		public final State state;
		public final T value;

		public StreamState(State state, T value) {
			this.state = state;
			this.value = value;
		}

		public static <T> StreamState<T> create(State state, T value) {
			return new StreamState<T>(state, value);
		}

		public static <T> StreamState<T> data(T value) {
			return create(State.MORE, value);
		}

		public static <T> StreamState<T> end() {
			return create(State.ENDED, null);
		}
	}

	public static enum State {
		MORE, ENDED
	}

	public static <T> Stream simpleValue(T value, Function<T, byte[]> toByteArray) {
		AtomicBoolean done = new AtomicBoolean(false);
		GenericStream<T> stream = new GenericStream<T>() {

			@Override
			protected StreamState<T> next() throws Exception {
				if (done.get()) {
					return StreamState.end();
				}
				done.set(true);
				return StreamState.data(value);
			}

			@Override
			protected Buffer serialize(T n) throws Exception {
				return new Buffer(toByteArray.apply(n));
			}
		};

		return VertxStream.stream(stream);

	}

}
