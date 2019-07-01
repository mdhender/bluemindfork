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
package net.bluemind.imap;

import java.io.Closeable;
import java.io.IOException;

import com.google.common.io.ByteSource;
import net.bluemind.common.io.FileBackedOutputStream;

public abstract class IMAPByteSource implements Closeable {
	public abstract ByteSource source();

	public abstract void close();

	public int size() {
		try {
			return (int) source().size();
		} catch (IOException e) {
			return 0;
		}
	}

	public static IMAPByteSource wrap(final FileBackedOutputStream fbos) {
		return new IMAPByteSource() {

			@Override
			public ByteSource source() {
				return fbos.asByteSource();
			}

			@Override
			public void close() {
				try {
					fbos.reset();
				} catch (IOException e) {
				}
			}
		};
	}

	public static IMAPByteSource wrap(final FileBackedOutputStream fbos, final int size) {
		return new IMAPByteSource() {

			@Override
			public ByteSource source() {
				return fbos.asByteSource();
			}

			@Override
			public void close() {
				try {
					fbos.reset();
				} catch (IOException e) {
				}
			}

			@Override
			public int size() {
				return size;
			}
		};
	}

	public static IMAPByteSource wrap(final ByteSource simple) {
		return new IMAPByteSource() {

			@Override
			public ByteSource source() {
				return simple;
			}

			@Override
			public void close() {
			}
		};
	}

	public static IMAPByteSource wrap(byte[] simple) {
		final ByteSource bs = ByteSource.wrap(simple);
		return new IMAPByteSource() {

			@Override
			public ByteSource source() {
				return bs;
			}

			@Override
			public void close() {
			}
		};
	}
}
