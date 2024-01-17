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

/**
 * See MS-ASDTYPE 2.7.6 TimeZone
 *
 */
public class TimeZoneCodec {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(TimeZoneCodec.class);

	public static EASTimeZone decode(String b64String) {
		ByteBuf base64Buf = Unpooled.wrappedBuffer(b64String.getBytes(StandardCharsets.US_ASCII));
		ByteBuf buf = Base64.decode(base64Buf).order(ByteOrder.LITTLE_ENDIAN);
		base64Buf.release();

		// Bias (4 bytes): The value of this field is a LONG, as specified in [MS-DTYP]
		// section 2.2.27. The offset from UTC, in minutes. For example, the bias for
		// Pacific Time (UTC-8) is 480.
		int bias = buf.readInt();

		// StandardName (64 bytes): The value of this field is an array of 32 WCHARs, as
		// specified in [MS- DTYP] section 2.2.60. It contains an optional description
		// for standard time. Any unused WCHARs in the array MUST be set to 0x0000.
		ByteBuf uncutString = Unpooled.buffer(64).order(ByteOrder.LITTLE_ENDIAN);
		buf.readBytes(uncutString);
		String standardName = asString(uncutString, 64);

		// StandardDate (16 bytes): The value of this field is a SYSTEMTIME structure,
		// as specified in [MS- DTYP] section 2.3.13. It contains the date and time when
		// the transition from DST to standard time occurs.
		SystemTime standardDate = SystemTime.of(buf);

		// StandardBias (4 bytes): The value of this field is a LONG. It contains the
		// number of minutes to add to the value of the Bias field during standard time.
		int standardBias = buf.readInt();

		// DaylightName (64 bytes): The value of this field is an array of 32 WCHARs. It
		// contains an optional description for DST. Any unused WCHARs in the array MUST
		// be set to 0x0000.
		uncutString = Unpooled.buffer(64).order(ByteOrder.LITTLE_ENDIAN);
		buf.readBytes(uncutString);
		String daylightName = asString(uncutString, 64);

		// DaylightDate (16 bytes): The value of this field is a SYSTEMTIME structure.
		// It contains the date and time when the transition from standard time to DST
		// occurs.
		SystemTime daylightDate = SystemTime.of(buf);

		// DaylightBias (4 bytes): The value of this field is a LONG. It contains the
		// number of minutes to add to the value of the Bias field during DST.
		int daylightBias = buf.readInt();

		buf.release();

		return new EASTimeZone(bias, standardName, standardDate, standardBias, daylightName, daylightDate,
				daylightBias);

	}

	private static String asString(ByteBuf uncutString, int length) {
		ByteBuf utf16String = Unpooled.buffer().order(ByteOrder.LITTLE_ENDIAN);
		byte[] out = new byte[length];
		uncutString.readBytes(out);
		utf16String.writeBytes(out);
		uncutString.release();

		return utf16String.toString(StandardCharsets.UTF_16LE).trim();
	}

}
