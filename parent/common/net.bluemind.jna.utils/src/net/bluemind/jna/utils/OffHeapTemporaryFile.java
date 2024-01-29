/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2024
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
package net.bluemind.jna.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class OffHeapTemporaryFile implements AutoCloseable {

	@SuppressWarnings("serial")
	public static class OffHeapException extends RuntimeException {

		public OffHeapException(Throwable e) {
			super(e);
		}

	}

	private final Path path;
	private final OneTimeClose close;

	public OffHeapTemporaryFile(int fd, OneTimeClose closeCallback) {
		this.path = Paths.get("/dev/fd/" + fd);
		this.close = closeCallback;
	}

	public InputStream openForReading() throws IOException {
		return Files.newInputStream(path);
	}

	public OutputStream openForWriting() throws IOException {
		return Files.newOutputStream(path);
	}

	public long length() {
		try {
			return Files.size(path);
		} catch (IOException e) {
			throw new OffHeapException(e);
		}
	}

	@Override
	public void close() {
		close.run();
	}

}
