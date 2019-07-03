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
package net.bluemind.mime4j.bodies.internal;

import java.io.IOException;
import java.io.InputStream;

import org.apache.james.mime4j.storage.Storage;
import org.apache.james.mime4j.storage.StorageOutputStream;
import org.apache.james.mime4j.storage.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteStreams;
import com.google.common.io.CountingOutputStream;

public class DiscardBodyStorageProvider implements StorageProvider {

	private static final Logger logger = LoggerFactory.getLogger(DiscardBodyStorageProvider.class);

	public static final class DiscardedStorage implements Storage {

		private final CountingOutputStream output;

		public DiscardedStorage() {
			output = new CountingOutputStream(ByteStreams.nullOutputStream());
		}

		public void store(InputStream in) throws IOException {
			ByteStreams.copy(in, output);
			logger.debug("Discarded stream {}bytes.", output.getCount());
		}

		public void write(byte[] buffer, int offset, int length) throws IOException {
			logger.debug("Discarded buffer {}bytes", length);
			output.write(buffer, offset, length);
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return new ZeroInputStream(output.getCount());
		}

		public long size() {
			return output.getCount();
		}

		@Override
		public void delete() {
			logger.debug("delete");
		}

	}

	private static final class ZeroInputStream extends InputStream {
		private long read;
		private final long goal;

		public ZeroInputStream(long size) {
			this.goal = size;
		}

		@Override
		public int read() throws IOException {
			int ret = read++ < goal ? 0 : -1;
			return ret;
		}

		@Override
		public long skip(long n) throws IOException {
			read += n;
			return n;
		}

		@Override
		public int available() throws IOException {
			int ret = (int) (goal - read);
			return ret;
		}

	}

	private static class DiscardOutputStream extends StorageOutputStream {

		private final DiscardedStorage discarded;

		public DiscardOutputStream() {
			this.discarded = new DiscardedStorage();
		}

		@Override
		protected void write0(byte[] buffer, int offset, int length) throws IOException {
			discarded.write(buffer, offset, length);
		}

		@Override
		protected Storage toStorage0() throws IOException {
			return discarded;
		}

	}

	@Override
	public DiscardedStorage store(InputStream in) throws IOException {
		DiscardedStorage ret = new DiscardedStorage();
		ret.store(in);
		return ret;
	}

	@Override
	public StorageOutputStream createStorageOutputStream() throws IOException {
		return new DiscardOutputStream();
	}

}
