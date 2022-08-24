/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.imap.endpoint.parsing;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.vertx.core.Handler;
import net.bluemind.imap.endpoint.EndpointRuntimeException;
import net.bluemind.imap.endpoint.cmd.RawImapCommand;
import net.bluemind.imap.endpoint.parsing.Part.Type;

public class ImapRequestParser {

	private static final Path TMP = Paths.get(System.getProperty("java.io.tmpdir"));

	private static final Logger logger = LoggerFactory.getLogger(ImapRequestParser.class);

	private final ArrayDeque<Part> parts;
	private final Handler<RawImapCommand> cmdHandler;

	private Part prev;

	public ImapRequestParser(Handler<RawImapCommand> parsedCmdHandler) {
		this.cmdHandler = parsedCmdHandler;
		parts = new ArrayDeque<>();
	}

	public void parse(Part p) {
		logger.debug("Got {}", p);
		Part current = p;

		// merge literals as single part
		if (p.type() == Type.LITERAL_CHUNK) {
			// first chunk of literal
			if (prev.type() == Type.COMMAND) {
				ByteBuf chunk = null;
				int fullSize = current.buffer().readableBytes() + p.expected();
				if (fullSize > 512_000) {
					chunk = mmap(fullSize);
				} else {
					chunk = Unpooled.buffer(fullSize, fullSize);
				}
				chunk.writeBytes(current.buffer());
				current = Part.literalChunk(chunk, p.expected());
				parts.add(current);
				prev = current;
			} else {
				prev.buffer().writeBytes(current.buffer());
			}
		} else {
			parts.add(current);
			prev = current;
			if (!current.continued()) {
				// parts represent a full command
				ArrayList<Part> copy = new ArrayList<>(parts);
				parts.clear();
				RawImapCommand cmd = new RawImapCommand(copy);
				cmdHandler.handle(cmd);
			}
		}

	}

	public void close() {
		parts.removeIf(p -> {
			p.release();
			return true;
		});

	}

	private ByteBuf mmap(int capacity) {
		try {
			return mmap0(capacity);
		} catch (IOException e) {
			throw new EndpointRuntimeException(e);
		}
	}

	private ByteBuf mmap0(int capacity) throws IOException {

		Path backingFile = Files.createTempFile(TMP, "imap-literal", ".mmap");
		try (RandomAccessFile raf = new RandomAccessFile(backingFile.toFile(), "rw")) {
			raf.setLength(capacity);
			MappedByteBuffer targetBuffer = raf.getChannel().map(MapMode.READ_WRITE, 0, capacity);
			return Unpooled.wrappedBuffer(targetBuffer).writerIndex(0).readerIndex(0);
		} finally {
			Files.deleteIfExists(backingFile);
		}
	}

}
