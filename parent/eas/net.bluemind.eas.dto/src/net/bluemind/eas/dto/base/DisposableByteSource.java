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
package net.bluemind.eas.dto.base;

import java.io.IOException;

import com.google.common.io.ByteSource;
import net.bluemind.common.io.FileBackedOutputStream;

public abstract class DisposableByteSource {

	public abstract ByteSource source();

	public abstract void dispose();

	public int size() {
		try {
			return (int) source().size();
		} catch (IOException e) {
			return 0;
		}
	}

	public static DisposableByteSource wrap(final FileBackedOutputStream fbos) {
		return new DisposableByteSource() {

			@Override
			public ByteSource source() {
				return fbos.asByteSource();
			}

			@Override
			public void dispose() {
				try {
					fbos.reset();
				} catch (IOException e) {
				}
			}
		};
	}

	public static DisposableByteSource wrap(final FileBackedOutputStream fbos, final int knownSize) {
		return new DisposableByteSource() {

			@Override
			public ByteSource source() {
				return fbos.asByteSource();
			}

			@Override
			public void dispose() {
				try {
					fbos.reset();
				} catch (IOException e) {
				}
			}

			@Override
			public int size() {
				return knownSize;
			}
		};
	}

	public static DisposableByteSource wrap(final ByteSource simple) {
		return new DisposableByteSource() {

			@Override
			public ByteSource source() {
				return simple;
			}

			@Override
			public void dispose() {
			}
		};
	}

	public static DisposableByteSource wrap(String s) {
		final byte[] bytes = s.getBytes();
		final ByteSource bs = ByteSource.wrap(bytes);
		return new DisposableByteSource() {

			@Override
			public ByteSource source() {
				return bs;
			}

			@Override
			public void dispose() {
			}

			@Override
			public int size() {
				return bytes.length;
			}
		};
	}

	public static DisposableByteSource wrap(final byte[] bytes) {
		final ByteSource bs = ByteSource.wrap(bytes);
		return new DisposableByteSource() {

			@Override
			public ByteSource source() {
				return bs;
			}

			@Override
			public void dispose() {
			}

			@Override
			public int size() {
				return bytes.length;
			}
		};
	}

}
