/*
 * Copyright (C) 2008 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package net.bluemind.common.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.io.ByteSource;

/**
 * An {@link OutputStream} that starts buffering to a byte array, but switches
 * to file buffering once the data reaches a configurable size.
 *
 * <p>
 * This class is thread-safe.
 *
 * @author Chris Nokleberg
 * @since 1.0
 */
public final class FileBackedOutputStream extends OutputStream {

	private final int fileThreshold;
	private final ByteSource source;
	private final String filenamePrefix;

	private OutputStream out;
	private MemoryOutput memory;
	private File file;

	/** ByteArrayOutputStream that exposes its internals. */
	private static class MemoryOutput extends ByteArrayOutputStream {
		byte[] getBuffer() {
			return buf;
		}

		int getCount() {
			return count;
		}
	}

	/** Returns the file holding the data (possibly null). */
	@VisibleForTesting
	synchronized File getFile() {
		return file;
	}

	/**
	 * Creates a new instance that uses the given file threshold, and does not reset
	 * the data when the {@link ByteSource} returned by {@link #asByteSource} is
	 * finalized.
	 *
	 * @param fileThreshold  the number of bytes before the stream should switch to
	 *                       buffering to a file
	 * @param filenamePrefix name hint for the optional temporary file
	 */
	public FileBackedOutputStream(int fileThreshold, String filenamePrefix) {
		this.fileThreshold = fileThreshold;
		this.filenamePrefix = filenamePrefix;
		memory = new MemoryOutput();
		out = memory;

		source = new ByteSource() {
			@Override
			public InputStream openStream() throws IOException {
				return openInputStream();
			}

			@Override
			public Optional<Long> sizeIfKnown() {
				return Optional.of(size());
			}

			@Override
			public long size() {
				return file != null ? file.length() : memory.getCount();
			}
		};
	}

	/**
	 * Returns a readable {@link ByteSource} view of the data that has been written
	 * to this stream.
	 *
	 * @since 15.0
	 */
	public ByteSource asByteSource() {
		return source;
	}

	private synchronized InputStream openInputStream() throws IOException {
		if (file != null) {
			return new FileInputStream(file);
		} else {
			return new ByteArrayInputStream(memory.getBuffer(), 0, memory.getCount());
		}
	}

	/**
	 * Calls {@link #close} if not already closed, and then resets this object back
	 * to its initial state, for reuse. If data was buffered to a file, it will be
	 * deleted.
	 *
	 * @throws IOException if an I/O error occurred while deleting the file buffer
	 */
	public synchronized void reset() throws IOException {
		try {
			close();
		} finally {
			if (memory == null) {
				memory = new MemoryOutput();
			} else {
				memory.reset();
			}
			out = memory;
			if (file != null) {
				File deleteMe = file;
				file = null;
				Files.delete(deleteMe.toPath());
			}
		}
	}

	@Override
	public synchronized void write(int b) throws IOException {
		update(1);
		out.write(b);
	}

	@Override
	public synchronized void write(byte[] b) throws IOException {
		write(b, 0, b.length);
	}

	@Override
	public synchronized void write(byte[] b, int off, int len) throws IOException {
		update(len);
		out.write(b, off, len);
	}

	@Override
	public synchronized void close() throws IOException {
		out.close();
	}

	@Override
	public synchronized void flush() throws IOException {
		out.flush();
	}

	/**
	 * Checks if writing {@code len} bytes would go over threshold, and switches to
	 * file buffering if so.
	 */
	private void update(int len) throws IOException {
		if (file == null && (memory.getCount() + len > fileThreshold)) {
			File temp = File.createTempFile("fbos-" + filenamePrefix, null);
			OutputStream transfer = Files.newOutputStream(temp.toPath()); // NOSONAR
			transfer.write(memory.getBuffer(), 0, memory.getCount());
			transfer.flush();

			// We've successfully transferred the data; switch to writing to
			// file
			out = transfer;
			file = temp;
			memory = null;
		}
	}
}
