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

import static org.junit.Assert.assertEquals;

import java.nio.charset.StandardCharsets;

import org.junit.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.bluemind.imap.vertx.utils.LiteralSize;

public class LiteralSizeTests {

	public static final int CNT = Integer.MAX_VALUE / 50 - 1;

	@Test
	public void testSpeedAndValue() {
		String s = "* 4 FETCH (UID 4 BODY[2] {398276}";
		ByteBuf b = Unpooled.wrappedBuffer(s.getBytes(StandardCharsets.US_ASCII));

		long bbTime = System.currentTimeMillis();
		for (int i = 0; i < CNT; i++) {
			int valueByBuf = LiteralSize.of(b);
			assertEquals(398276, valueByBuf);
		}
		bbTime = System.currentTimeMillis() - bbTime;

		System.out.println("byBuf: " + bbTime + "ms.");
	}

}
