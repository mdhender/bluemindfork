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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.streams.ReadStream;

import com.google.common.io.CountingInputStream;

import net.bluemind.core.api.Stream;
import net.bluemind.core.rest.vertx.VertxStream;

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
		private final OutputStream fileOut;
		private boolean closed;
		private boolean reset;

		public ResetableOutput(File f) {
			this.file = f.toPath();
			try {
				this.fileOut = Files.newOutputStream(file);
			} catch (IOException e) {
				throw new AdaptException(e);
			}
		}

		public void write(byte[] data) throws IOException {
			fileOut.write(data);
		}

		public void close() {
			try {
				fileOut.flush();
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

	private static boolean shm = isShmAvailable();

	private static boolean isShmAvailable() {
		File shm = new File("/dev/shm");
		boolean ret = shm.exists() && shm.isDirectory();
		if (ret) {
			File out = new File(shm, "sync.bodies");
			out.mkdirs();
		}
		return ret;
	}

	private static final AtomicLong tmpName = new AtomicLong();

	private static ResetableOutput output() {
		if (shm) {
			File f = new File("/dev/shm/sync.bodies/body." + tmpName.incrementAndGet());
			return new ResetableOutput(f);
		} else {
			try {
				return new ResetableOutput(File.createTempFile("ez-is-adapt", ".stream"));
			} catch (IOException e) {
				throw new AdaptException(e);
			}
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
			ReadStream<?> vxStream = VertxStream.read(stream);
			setupStreamHandlers(streamConsumer, ret, vxStream);
		} catch (Exception e) {
			logger.error("Error setting up stream handlers " + e.getMessage(), e);
			ret.completeExceptionally(e);
		}
		return ret;
	}

	private static <T> void setupStreamHandlers(final Function<CountingInputStream, T> streamConsumer,
			CompletableFuture<T> ret, ReadStream<?> vxStream) {
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
		vxStream.dataHandler(buf -> {
			byte[] toAdd = buf.getBytes();
			try {
				diskCopy.write(toAdd);
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
