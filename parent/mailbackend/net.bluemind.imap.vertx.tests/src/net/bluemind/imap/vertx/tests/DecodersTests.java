/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.imap.vertx.tests;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.james.mime4j.codec.QuotedPrintableOutputStream;
import org.junit.Test;

import com.google.common.io.ByteStreams;

import io.netty.buffer.ByteBuf;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;
import net.bluemind.core.rest.base.GenericStream.AccumulatorStream;
import net.bluemind.imap.vertx.stream.Base64Decoder;
import net.bluemind.imap.vertx.stream.QuotedPrintableDecoder;

public class DecodersTests {

	Buffer res(String n) throws IOException {
		try (InputStream in = DecodersTests.class.getClassLoader().getResourceAsStream("data/" + n)) {
			return Buffer.buffer(ByteStreams.toByteArray(in));
		}
	}

	@Test
	public void testB64() throws IOException {
		Buffer qp = res("encoded.b64");

		for (int i = 0; i < 100; i++) {
			Buffer src = qp.copy();
			AccumulatorStream tgt = new AccumulatorStream();
			Base64Decoder dec = new Base64Decoder(tgt);
			splitWrite(src, dec);
			String decoded = tgt.buffer().toString(StandardCharsets.UTF_8);
			assertTrue(decoded.contains("b€douin"));
		}
	}

	@Test
	public void testQP() throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		QuotedPrintableOutputStream os = new QuotedPrintableOutputStream(out, false);
		Buffer plain = res("encoded.plain");
		os.write(plain.getBytes());
		os.close();
		Buffer qp = Buffer.buffer(out.toByteArray());
		System.err.println(new String(out.toByteArray()));

		for (int i = 0; i < 100; i++) {
			Buffer src = qp.copy();
			AccumulatorStream tgt = new AccumulatorStream();
			QuotedPrintableDecoder dec = new QuotedPrintableDecoder(tgt);
			splitWrite(src, dec);
			String decoded = tgt.buffer().toString(StandardCharsets.UTF_8);
			assertTrue(decoded.contains("les voilà bientôt"));
		}
	}

	private void splitWrite(Buffer source, WriteStream<Buffer> target) {
		ByteBuf src = source.getByteBuf();
		ThreadLocalRandom rand = ThreadLocalRandom.current();
		while (src.readableBytes() > 0) {
			ByteBuf slice = src.readSlice(rand.nextInt(src.readableBytes()) + 1);
			Buffer sliceBuf = Buffer.buffer(slice);
			target.write(sliceBuf);
		}
	}

}
