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
package net.bluemind.lib.globalid;

import java.nio.charset.Charset;

public final class ExtIdConverter {

	private static final String byteArrayId = "040000008200E00074C5B7101A82E008";
	private static final String dataPrefix = "7643616C2D55696401000000"; // vCal-Uid
	private static int thirdPartyGlobalIdMinSize = 104;

	/**
	 * Convert an external event uid to its stringified globalObjectId form.
	 * 
	 * @param extId
	 * @return
	 */
	public static final String fromExtId(String extId) {
		if (extId == null) {
			throw new IllegalArgumentException("extId must not be null");
		}
		if (extId.length() >= 82 && extId.startsWith(byteArrayId)) {
			return extId;
		}
		byte[] bytes = extId.getBytes(Charset.forName("UTF-8"));
		String thirdPartyGlobalId = toHexString(bytes);
		StringBuilder sb = new StringBuilder();
		sb.append(byteArrayId);
		sb.append("00000000"); // InstanceDate
		sb.append("0000000000000000"); // CreationDateTime
		sb.append("0000000000000000"); // Padding
		int size = (dataPrefix.length() / 2 + bytes.length + 1);
		sb.append(Integer.toHexString(size).toUpperCase());
		sb.append("000000"); // DataSize
		sb.append(dataPrefix).append(thirdPartyGlobalId).append("00");
		return sb.toString();
	}

	public static final String toExtId(String gaid) {
		String hexSize = csSub(gaid, thirdPartyGlobalIdMinSize - dataPrefix.length() - 8, 2);
		int size = Integer.parseInt(hexSize, 16);
		String hexExtId = csSub(gaid, thirdPartyGlobalIdMinSize, size * 2 - dataPrefix.length() - 2);
		return hexString2ascii(hexExtId);
	}

	private static String hexString2ascii(String hexExtId) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i <= hexExtId.length() - 2; i += 2) {
			String sub = csSub(hexExtId, i, 2);
			int toAppend = Integer.parseInt(sub, 16);
			sb.append((char) toAppend);
		}
		return sb.toString();
	}

	public static final boolean isThirdPartyGlobalId(String gaid) {
		return gaid.startsWith(byteArrayId) && gaid.length() > thirdPartyGlobalIdMinSize
				&& csSub(gaid, thirdPartyGlobalIdMinSize - dataPrefix.length(), dataPrefix.length()).equals(dataPrefix);
	}

	/**
	 * c-sharp like substring to ease porting from Blue Mind Outlook connector.
	 * 
	 * @param s
	 * @param idx
	 * @param len
	 * @return
	 */
	private static final String csSub(String s, int idx, int len) {
		return s.substring(idx, idx + len);
	}

	public static byte[] fromHexString(String hexGlobId) {
		byte[] ret = new byte[hexGlobId.length() / 2];
		for (int i = 0; i <= hexGlobId.length() - 2; i += 2) {
			String sub = csSub(hexGlobId, i, 2);
			int toAppend = Integer.parseInt(sub, 16);
			ret[i / 2] = (byte) toAppend;
		}
		return ret;
	}

	public static String toHexString(byte[] bytes) {
		StringBuilder s = new StringBuilder();
		int len = bytes.length;
		for (int i = 0; i < len; i++) {
			String b = Integer.toHexString(bytes[i] & 0xFF).toUpperCase();
			if (b.length() == 1) {
				s.append('0');
			}
			s.append(b);
		}
		return s.toString();
	}
}
