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
import static org.junit.Assert.assertTrue;

import java.nio.charset.Charset;

import org.junit.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.bluemind.lmtp.impl.SMTPByteBufUtils;

public class TransparentCharSMTPTest {

	@Test
	public void testIsoAndSpeedAndMemory() {
		String mail = "hello\n" //
				+ "test\n" //
				+ ".destest\n" //
				+ ".destest2XX\n" //
				+ ".";

		ByteBuf mailByteBuf = Unpooled.wrappedBuffer(mail.getBytes());
		SMTPByteBufUtils.transformToSMTP(Unpooled.wrappedBuffer(mail.getBytes()));
		long time = System.currentTimeMillis();
		for (int i = 0; i < 1000 * 1000; i++) {
			SMTPByteBufUtils.transformFromSMTP(SMTPByteBufUtils.transformToSMTP(mailByteBuf));
		}
		assertTrue((System.currentTimeMillis() - time) < 1000 * 5);
		assertEquals(mail, SMTPByteBufUtils.transformFromSMTP(SMTPByteBufUtils.transformToSMTP(mailByteBuf))
				.toString(Charset.defaultCharset()));
	}

	@Test
	public void testTransformToSMTP() {
		String mail = "hello\n" //
				+ "test\n" //
				+ ".destest\n" //
				+ ".destest2XX\n" //
				+ ".";

		String expected = "hello\n" //
				+ "test\n" //
				+ "..destest\n" //
				+ "..destest2XX\n" //
				+ ".";
		ByteBuf res = SMTPByteBufUtils.transformToSMTP(Unpooled.wrappedBuffer(mail.getBytes()));
		assertEquals(expected, res.toString(Charset.defaultCharset()));
	}

	@Test
	public void testTransformFromSMTP() {

		String mail = "hello\n" //
				+ "test\n" //
				+ "..destest\n" //
				+ "..destest2XX\n" //
				+ ".";
		String expected = "hello\n" //
				+ "test\n" //
				+ ".destest\n" //
				+ ".destest2XX\n" //
				+ ".";

		ByteBuf res = SMTPByteBufUtils.transformFromSMTP(Unpooled.wrappedBuffer(mail.getBytes()));
		assertEquals(expected, res.toString(Charset.defaultCharset()));
	}
}
