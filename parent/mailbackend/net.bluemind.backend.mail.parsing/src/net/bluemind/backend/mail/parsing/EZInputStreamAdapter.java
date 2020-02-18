/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.backend.mail.parsing;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.CountingInputStream;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;
import net.bluemind.core.api.Stream;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.core.rest.vertx.VertxStream.LocalPathStream;

public class EZInputStreamAdapter {

	private static final Logger logger = LoggerFactory.getLogger(EZInputStreamAdapter.class);

	@SuppressWarnings("serial")
	private static class AdaptException extends RuntimeException {
		public AdaptException(Throwable t) {
			super(t);
		}
	}

	private static class ResetableOutput {
		private final Path file;
		private final SeekableByteChannel fileOut;
		private boolean closed;
		private boolean reset;

		public ResetableOutput(Path path) {
			this.file = path;
			try {
				this.fileOut = Files.newByteChannel(file, StandardOpenOption.CREATE,
						StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
			} catch (IOException e) {
				throw new AdaptException(e);
			}
		}

		public void write(ByteBuffer data) throws IOException {
			fileOut.write(data);
		}

		public void close() {
			try {
				fileOut.close();
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
			closed = true;
		}

		public InputStream input() throws IOException {
			return Files.newInputStream(file);
		}

		public void reset() {
			try {
				Files.delete(file);
			} catch (IOException e) {
			}
			reset = true;
		}

		@Override
		protected void finalize() throws Throwable { // NOSONAR
			if (!closed) {
				logger.warn("Closing {} from finalize", this);
				close();
			}
			if (!reset) {
				logger.warn("Reset {} from finalize", this);
				reset();
			}
			super.finalize();
		}

	}

	private static ResetableOutput output() {
		try {
			return new ResetableOutput(Files.createTempFile("ez-is-adapt", ".stream"));
		} catch (IOException e) {
			throw new AdaptException(e);
		}
	}

	/**
	 * Provides an InputStream from a bluemind {@link Stream}.
	 * 
	 * @param stream         the stream to adapt as in {@link InputStream}
	 * @param streamConsumer the consumer will get a {@link CountingInputStream} and
	 *                       can safely perform blocking reads as it is invoked from
	 *                       a dedicated thread.
	 * 
	 * @return
	 */
	public static <T> CompletableFuture<T> consume(Stream stream,
			final Function<CountingInputStream, T> streamConsumer) {

		CompletableFuture<T> ret = new CompletableFuture<>();
		try {
			ReadStream<Buffer> vxStream = VertxStream.read(stream);
			setupStreamHandlers(streamConsumer, ret, vxStream);
		} catch (Exception e) {
			logger.error("Error setting up stream handlers " + e.getMessage(), e);
			ret.completeExceptionally(e);
		}
		return ret;
	}

	private static <T> void setupStreamHandlers(final Function<CountingInputStream, T> streamConsumer,
			CompletableFuture<T> ret, ReadStream<Buffer> vxStream) {
		if (vxStream instanceof LocalPathStream) {
			LocalPathStream lps = (LocalPathStream) vxStream;
			try (CountingInputStream toConsume = new CountingInputStream(Files.newInputStream(lps.path()))) {
				T output = streamConsumer.apply(toConsume);
				ret.complete(output);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				ret.completeExceptionally(e);
			}
			return;
		}

		ResetableOutput diskCopy = output();

		vxStream.endHandler(gotIt -> {
			diskCopy.close();
			try (CountingInputStream toConsume = new CountingInputStream(diskCopy.input())) {
				T output = streamConsumer.apply(toConsume);
				ret.complete(output);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				ret.completeExceptionally(e);
			} finally {
				diskCopy.reset();
			}

		});
		vxStream.handler(buf -> {
			try {
				diskCopy.write(buf.getByteBuf().nioBuffer());
			} catch (IOException e) {
				logger.error(e.getMessage());
			}
		});
		vxStream.exceptionHandler(t -> {
			logger.error(t.getMessage(), t);
			diskCopy.close();
			diskCopy.reset();
		});

		logger.debug("resume {}...", vxStream);
		vxStream.resume();
	}

}
