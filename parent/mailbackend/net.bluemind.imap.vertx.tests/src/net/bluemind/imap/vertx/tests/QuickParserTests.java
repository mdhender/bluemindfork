/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.imap.vertx.tests;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.junit.Test;

import io.netty.buffer.Unpooled;
import net.bluemind.imap.vertx.utils.LiteralSize;
import net.bluemind.imap.vertx.utils.QuickParser;
import net.bluemind.imap.vertx.utils.QuickParser.BinStream;
import net.bluemind.imap.vertx.utils.QuickParser.TextChunk;

public class QuickParserTests {

	Pattern literal = Pattern.compile(".+\\{(\\d+)\\}");

	@Test
	public void testSome() {
		Consumer<BinStream> binaries = stream -> {
			System.out.println("Got stream of " + stream.expectedSize() + " bytes");
			stream.dataHandler(b -> {
				System.out.println("BIN: " + b.readableBytes());
			});
			stream.endHandler(v -> {
				System.out.println("BIN-END.");
			});
		};
		Consumer<TextChunk> text = chunk -> {
			String str = chunk.buf.toString(StandardCharsets.UTF_8);
			System.out.println("TEXT: '" + str + "'");
			int litSize = LiteralSize.of(chunk.buf);
			if (litSize > 0) {
				System.out.println("literal follows with " + litSize + " bytes");
				chunk.parser.fixed(litSize);
			}
		};
		QuickParser qp = new QuickParser(Unpooled.wrappedBuffer("\r\n".getBytes()), text, binaries);
		qp.handle(Unpooled.wrappedBuffer("* YEAH\r\nA1 OK good.\r\n".getBytes()));
		qp.handle(Unpooled.wrappedBuffer("* 4 FETCH (UID 4 BODY[3] {2}\r\naa)\r\n".getBytes()));
	}

}
