/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.eas.backend;

import java.io.IOException;
import java.io.InputStream;

import com.google.common.io.ByteSource;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.vertx.core.buffer.Buffer;

public final class BufferByteSource {

	public static interface IBufferHolder {
		ByteBuf nettyBuffer();
	}

	private BufferByteSource() {
	}

	public static ByteSource of(Buffer b) {
		return of(b.getByteBuf());
	}

	public static ByteSource of(byte[] b) {
		return of(Unpooled.wrappedBuffer(b));
	}

	private static class BufferSource extends ByteSource implements IBufferHolder {

		private final ByteBuf buf;
		private final int len;

		public BufferSource(ByteBuf b) {
			this.buf = b;
			this.len = b.readableBytes();
		}

		@Override
		public InputStream openStream() throws IOException {
			return new ByteBufInputStream(buf.duplicate());
		}

		@Override
		public long size() {
			return len;
		}

		@Override
		public ByteBuf nettyBuffer() {
			return buf.duplicate();
		}

	}

	public static ByteSource of(ByteBuf b) {
		return new BufferSource(b);
	}

}
