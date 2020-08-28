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
package net.bluemind.eas.timezone;

import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.base64.Base64;

public class TimeZoneCodec {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(TimeZoneCodec.class);

	public static EASTimeZone decode(String b64String) {
		ByteBuf base64Buf = Unpooled.wrappedBuffer(b64String.getBytes(StandardCharsets.US_ASCII));
		ByteBuf buf = Base64.decode(base64Buf).order(ByteOrder.LITTLE_ENDIAN);
		base64Buf.release();

		int bias = buf.readInt();
		ByteBuf uncutString = Unpooled.buffer(64).order(ByteOrder.LITTLE_ENDIAN);
		buf.readBytes(uncutString);
		String standardName = asString(uncutString);
		SystemTime standardDate = SystemTime.of(buf);
		int standardBias = buf.readInt();

		uncutString = Unpooled.buffer(64).order(ByteOrder.LITTLE_ENDIAN);
		buf.readBytes(uncutString);
		String daylightName = asString(uncutString);
		SystemTime daylightDate = SystemTime.of(buf);
		int daylightBias = buf.readInt();
		buf.release();
		return new EASTimeZone(bias, standardName, standardDate, standardBias, daylightName, daylightDate,
				daylightBias);

	}

	private static String asString(ByteBuf uncutString) {
		ByteBuf utf16String = Unpooled.buffer().order(ByteOrder.LITTLE_ENDIAN);
		byte[] out = new byte[2];
		do {
			uncutString.readBytes(out);
			if (out[0] == 0 && out[1] == 0) {
				break;
			} else {
				utf16String.writeBytes(out);
			}
		} while (true);
		uncutString.release();

		return utf16String.toString(StandardCharsets.UTF_16LE);
	}

}
