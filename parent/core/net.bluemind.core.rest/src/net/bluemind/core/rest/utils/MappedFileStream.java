/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.core.rest.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import io.netty.buffer.ByteBuf;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;
import net.bluemind.core.api.Stream;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.lib.vertx.utils.MmapWriteStream;

public class MappedFileStream {

	private MappedFileStream() {
	}

	public static CompletableFuture<ByteBuf> createMappedFile(Stream stream, int sizeHint) {
		File dir = new File(System.getProperty("java.io.tmpdir"));

		return readMmap(dir.toPath(), stream, sizeHint);
	}

	private static CompletableFuture<ByteBuf> readMmap(Path path, Stream s, int sizeHint) {
		try {
			MmapWriteStream out = new MmapWriteStream(path, sizeHint);
			ReadStream<Buffer> toRead = VertxStream.read(s);
			toRead.pipeTo(out);
			toRead.resume();
			return out.mmap();
		} catch (IOException e) {
			CompletableFuture<ByteBuf> ex = new CompletableFuture<>();
			ex.completeExceptionally(e);
			return ex;
		}
	}

}
