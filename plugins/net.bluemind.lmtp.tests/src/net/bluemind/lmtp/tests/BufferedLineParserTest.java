/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.lmtp.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Random;

import org.apache.james.mime4j.dom.Message;
import org.junit.Test;

import com.google.common.base.Strings;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.vertx.core.buffer.Buffer;
import net.bluemind.lmtp.parser.BufferedLineParser;
import net.bluemind.mime4j.common.Mime4JHelper;

public class BufferedLineParserTest {

	@Test
	public void testDelimiters() {
		String fullDialog = ""//
				+ "MAIL FROM: toto\r\n" //
				+ "RCPT TO: titi\r\n"//
				+ "DATA\r\n" + "Return-Path: toto\r\n"//
				+ genLotOfHeaders()//
				+ "From: toto\r\n"//
				+ "Subject: roberto\r\n\r\n"//
				+ "blabla\r\n" + genBodyCrap() + "body\r\n.\r\nQUIT\r\n";

		BufferedLineParser parser = new BufferedLineParser();
		parser.setDelimitedMode("\r\n");

		ByteBuf asBuffer = Unpooled.wrappedBuffer(fullDialog.getBytes());
		Random r = new Random();
		boolean dataMode = false;
		String body = null;
		int cmdCount = 0;
		while (asBuffer.readableBytes() > 0) {
			int some = 1 + r.nextInt(32);
			ByteBuf slice = asBuffer.readSlice(Math.min(asBuffer.readableBytes(), some));
			System.out.println("Feed with " + slice.readableBytes() + "bytes");
			parser.feed(Buffer.buffer(slice));
			Buffer next = parser.next();
			while (next != BufferedLineParser.NEED_MORE && next != null) {
				String chunk = next.toString();
				if (dataMode) {
					body = chunk;
					parser.setDelimitedMode("\r\n");
					dataMode = false;
					System.err.println("Back from data mode, body is " + body.length() + " char(s)");
				} else {
					System.out.println("Got CMD chunk '" + chunk + "'");
					cmdCount++;
					if ("DATA".equals(chunk)) {
						System.err.println("Switch to data mode.");
						dataMode = true;
						parser.setDelimitedMode("\r\n.\r\n");
					}
				}

				next = parser.next();
			}
		}
		assertNotNull(body);
		System.out.println("CMD count: " + cmdCount);
		assertEquals(4, cmdCount);
		Message parsed = Mime4JHelper.parse(body.getBytes());
		assertNotNull(parsed);
		assertEquals("roberto", parsed.getSubject());
	}

	private String genLotOfHeaders() {
		Random r = new Random();
		int hCount = r.nextInt(512);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < hCount; i++) {
			String h = "HeaderN" + Strings.padStart(Integer.toString(i), 3, '0') + ": value " + r.nextInt() + "\r\n";
			sb.append(h);
		}
		return sb.toString();
	}

	private String genBodyCrap() {
		Random r = new Random();
		int hCount = 512 + r.nextInt(1024 * 1024);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < hCount; i++) {
			String h = "Line " + Strings.padStart(Integer.toString(i), 3, '0') + " / " + hCount + "\r\n";
			sb.append(h);
		}
		return sb.toString();
	}

}
