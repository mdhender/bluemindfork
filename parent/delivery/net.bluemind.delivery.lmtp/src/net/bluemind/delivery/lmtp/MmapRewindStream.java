/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.delivery.lmtp;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Files;
import java.nio.file.Path;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class MmapRewindStream {

	private final MappedByteBuffer targetBuffer;
	private final ByteBuf wrapped;

	public MmapRewindStream(InputStream data, long capacity) throws IOException {
		Path backingFile = Files.createTempFile("lmtp-data", ".mmap");
		try (RandomAccessFile raf = new RandomAccessFile(backingFile.toFile(), "rw")) {
			raf.setLength(capacity);
			this.targetBuffer = raf.getChannel().map(MapMode.READ_WRITE, 0, capacity);
			this.wrapped = Unpooled.wrappedBuffer(targetBuffer);
			this.wrapped.writerIndex(0).readerIndex(0);
			this.wrapped.writeBytes(data.readAllBytes());
		}
		Files.deleteIfExists(backingFile);
	}

	public ByteBuf byteBuffer() {
		return this.wrapped.duplicate().asReadOnly();
	}

	public ByteBuf byteBufRewinded() {
		this.wrapped.readerIndex(0);
		return byteBuffer();
	}

}
