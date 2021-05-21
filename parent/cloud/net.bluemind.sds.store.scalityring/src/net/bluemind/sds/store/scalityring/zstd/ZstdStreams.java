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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.sds.store.scalityring.zstd;

import java.io.File;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.luben.zstd.ZstdInputStream;
import com.github.luben.zstd.ZstdOutputStream;
import com.google.common.io.ByteStreams;
import com.google.common.io.CountingInputStream;

import io.netty.util.concurrent.DefaultThreadFactory;

public class ZstdStreams {

	private ZstdStreams() {

	}

	private static final Logger logger = LoggerFactory.getLogger(ZstdStreams.class);
	private static final ExecutorService compressPool = Executors
			.newCachedThreadPool(new DefaultThreadFactory("zstd-for-scality"));

	@SuppressWarnings("serial")
	public static class ZstdScalityException extends RuntimeException {
		public ZstdScalityException(Throwable t) {
			super(t);
		}
	}

	/**
	 * 
	 * The returned input stream provides zstd-compressed data stream for the given
	 * source file
	 * 
	 * @param source
	 * @return
	 */
	public static CountingInputStream compress(File source) {
		Path path = source.toPath();
		PipedInputStream readCompressed = new PipedInputStream(32768);
		PipedOutputStream writePlain = new PipedOutputStream(); // NOSONAR
		try {
			writePlain.connect(readCompressed);
		} catch (IOException e) {
			throw new ZstdScalityException(e);
		}

		Callable<Void> compression = () -> {
			try (InputStream in = Files.newInputStream(path); ZstdOutputStream zos = new ZstdOutputStream(writePlain)) {
				ByteStreams.copy(in, zos);
			} catch (IOException ioe) {
				logger.error(ioe.getMessage(), ioe);
			} finally {
				writePlain.close();
			}

			return null;
		};
		compressPool.submit(compression);
		return new CountingInputStream(readCompressed);
	}

	/**
	 * The returned output stream will apply zstd decompression before writing to
	 * the given path
	 * 
	 * @param target
	 * @return
	 */
	public static OutputStream decompress(Path target) {
		PipedInputStream readPlain = new PipedInputStream(32768);
		PipedOutputStream writeCompressed = new PipedOutputStream(); // NOSONAR
		try {
			writeCompressed.connect(readPlain);
		} catch (IOException e) {
			try {
				readPlain.close();
				writeCompressed.close();
			} catch (IOException e1) {
				// OK
			}
			throw new ZstdScalityException(e);
		}

		Callable<Void> decomp = () -> {
			try (OutputStream out = Files.newOutputStream(target);
					ZstdInputStream dec = new ZstdInputStream(readPlain)) {
				ByteStreams.copy(dec, out);
			} catch (IOException ioe) {
				logger.error(ioe.getMessage(), ioe);
			}
			return null;
		};
		Future<Void> future = compressPool.submit(decomp);
		return new FilterOutputStream(writeCompressed) {
			@Override
			public void close() throws IOException {
				super.close();
				try {
					future.get();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				} catch (ExecutionException e) {
					logger.error(e.getMessage(), e);
				}
			}
		};
	}

}
