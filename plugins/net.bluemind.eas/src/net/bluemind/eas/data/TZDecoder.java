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
package net.bluemind.eas.data;

import java.nio.ByteOrder;
import java.util.TimeZone;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class TZDecoder {

	public TimeZone decode(String b64) {
		// Doc : [MS-ASDTYPE] 2.7 TimeZone
		// Doc about types :
		// http://msdn.microsoft.com/fr-fr/library/bb469811.aspx
		// 1 LONG = 4 bytes
		// 1 WCHAR = 2 bytes
		// 1 SYSTEMTIME = 8 SHORT = 8 X 2 bytes
		// TOTAL TIMEZONE STRUCT must be 172 bytes

		byte tzstruct[] = java.util.Base64.getDecoder().decode(b64);

		// Bias (4 bytes): The value of this field is a LONG, as specified in
		// [MS-DTYP]. The offset from UTC, in minutes. For example, the bias for
		// Pacific Time (UTC-8) is 480.
		ByteBuf biasBuf = Unpooled.copiedBuffer(tzstruct, 0, 4);
		int bias = biasBuf.order(ByteOrder.LITTLE_ENDIAN).readInt();

		// ByteBuffer bfStandardName = ByteBuffer.wrap(tzstruct, 4, 64);
		// ByteBuffer bfStandardDate = ByteBuffer.wrap(tzstruct, 68, 16);
		// ByteBuffer bfStandardBias = ByteBuffer.wrap(tzstruct, 84, 4);
		// ByteBuffer bfDaylightName = ByteBuffer.wrap(tzstruct, 88, 64);
		// ByteBuffer bfDaylightDate = ByteBuffer.wrap(tzstruct, 152, 16);
		// ByteBuffer bfDaylightBias = ByteBuffer.wrap(tzstruct, 168, 4);

		// NOT YET USED
		//
		// bfStandardBias.order(ByteOrder.LITTLE_ENDIAN);
		// int standardBias = bfStandardBias.getInt();
		//
		// bfDaylightBias.order(ByteOrder.LITTLE_ENDIAN);
		// int daylightBias = bfDaylightBias.getInt();

		int rawOffset = bias * 60 * 1000 * -1;

		TimeZone timezone = TimeZone.getDefault();
		timezone.setRawOffset(rawOffset);
		String timezones[] = TimeZone.getAvailableIDs(rawOffset);
		if (timezones.length > 0) {
			timezone = TimeZone.getTimeZone(timezones[0]);
		}

		// USEFUL DEBUG LINES
		//
		// StringBuffer sb = new StringBuffer();
		// for (int i = 0; i < 172; i+=1) {
		// sb.append(Byte.valueOf(tzstruct[i]).intValue());
		// }
		//
		// logger.info("b64: " + b64);
		// logger.info("tzstruct: "+ sb.toString());
		// logger.info("bias: " + bias);
		// logger.info("standardbias: " + standardBias);
		// logger.info("standardname: " +
		// bfStandardName.asCharBuffer().toString());
		// logger.info("daylightBias: " + daylightBias);

		return timezone;
	}

}
