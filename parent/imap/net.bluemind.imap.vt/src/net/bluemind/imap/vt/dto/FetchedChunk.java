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
package net.bluemind.imap.vt.dto;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import net.bluemind.jna.utils.OffHeapTemporaryFile;

public class FetchedChunk implements AutoCloseable {

	private static final FetchedChunk EMPTY = new FetchedChunk(null) {
		@Override
		public InputStream open() throws IOException {
			return new ByteArrayInputStream(new byte[0]);
		}

		@Override
		public int length() {
			return 0;
		}
	};

	public static FetchedChunk empty() {
		return EMPTY;
	}

	private final OffHeapTemporaryFile offHeap;

	public FetchedChunk(OffHeapTemporaryFile offHeap) {
		this.offHeap = offHeap;
	}

	public InputStream open() throws IOException {
		return offHeap.openForReading();
	}

	public int length() {
		return (int) offHeap.length();
	}

	@Override
	public void close() throws Exception {
		offHeap.close();
	}

}
