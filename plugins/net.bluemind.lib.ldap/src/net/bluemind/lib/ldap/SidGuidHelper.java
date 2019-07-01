/*BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012
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
 * END LICENSE
 */
package net.bluemind.lib.ldap;

import java.util.StringTokenizer;

import org.apache.commons.codec.binary.Hex;

public class SidGuidHelper {
	public static String convertGuidToString(byte[] bytes) {
		if (bytes == null || bytes.length != 16) {
			return null;
		}

		char[] hex = Hex.encodeHex(bytes);
		StringBuffer sb = new StringBuffer();
		sb.append('{');
		sb.append(hex, 6, 2);
		sb.append(hex, 4, 2);
		sb.append(hex, 2, 2);
		sb.append(hex, 0, 2);
		sb.append('-');
		sb.append(hex, 10, 2);
		sb.append(hex, 8, 2);
		sb.append('-');
		sb.append(hex, 14, 2);
		sb.append(hex, 12, 2);
		sb.append('-');
		sb.append(hex, 16, 4);
		sb.append('-');
		sb.append(hex, 20, 12);
		sb.append('}');

		if (sb.toString().trim().isEmpty()) {
			return null;
		}

		return sb.toString().toLowerCase();
	}

	public static boolean checkGuidSyntax(String guid) {
		if (guid.length() != 38) {
			return false;
		}

		if (!guid.matches("\\{\\p{XDigit}{8}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{12}\\}")) {
			return false;
		}

		return true;
	}

	public static String convertStringToGUidEscapedBytes(String guid) {
		if (!checkGuidSyntax(guid)) {
			return null;
		}

		guid = guid.substring(1, guid.length() - 1);

		StringBuffer sb = new StringBuffer();
		StringTokenizer st = new StringTokenizer(guid, "-");
		String temp, s1, s2, s3, s4, s5;
		temp = s1 = s2 = s3 = s4 = s5 = null;
		int i = 1;
		while (st.hasMoreTokens()) {
			temp = st.nextToken();
			switch (i) {
			case 1:
				s1 = temp;
				break;
			case 2:
				s2 = temp;
				break;
			case 3:
				s3 = temp;
				break;
			case 4:
				s4 = temp;
				break;
			case 5:
				s5 = temp;
				break;
			default:
				break;
			}
			i++;
		}

		sb.append("\\" + s1.substring(6, 8));
		sb.append("\\" + s1.substring(4, 6));
		sb.append("\\" + s1.substring(2, 4));
		sb.append("\\" + s1.substring(0, 2));

		sb.append("\\" + s2.substring(2, 4));
		sb.append("\\" + s2.substring(0, 2));

		sb.append("\\" + s3.substring(2, 4));
		sb.append("\\" + s3.substring(0, 2));

		sb.append("\\" + s4.substring(0, 2));
		sb.append("\\" + s4.substring(2, 4));

		sb.append("\\" + s5.substring(0, 2));
		sb.append("\\" + s5.substring(2, 4));
		sb.append("\\" + s5.substring(4, 6));
		sb.append("\\" + s5.substring(6, 8));
		sb.append("\\" + s5.substring(8, 10));
		sb.append("\\" + s5.substring(10, 12));

		return sb.toString();
	}

	public static String convertSidToString(byte[] bytes) {
		/*
		 * The binary data structure, from
		 * http://msdn.microsoft.com/en-us/library/cc230371(PROT.10).aspx: byte[0] -
		 * Revision (1 byte): An 8-bit unsigned integer that specifies the revision
		 * level of the SID structure. This value MUST be set to 0x01. byte[1] -
		 * SubAuthorityCount (1 byte): An 8-bit unsigned integer that specifies the
		 * number of elements in the SubAuthority array. The maximum number of elements
		 * allowed is 15. byte[2-7] - IdentifierAuthority (6 bytes): A
		 * SID_IDENTIFIER_AUTHORITY structure that contains information, which indicates
		 * the authority under which the SID was created. It describes the entity that
		 * created the SID and manages the account. Six element arrays of 8-bit unsigned
		 * integers that specify the top-level authority big-endian! and then -
		 * SubAuthority (variable): A variable length array of unsigned 32-bit integers
		 * that uniquely identifies a principal relative to the IdentifierAuthority. Its
		 * length is determined by SubAuthorityCount. little-endian!
		 */

		if (bytes == null || bytes.length < 8) {
			return null;
		}

		char[] hex = Hex.encodeHex(bytes);
		StringBuffer sb = new StringBuffer();

		// start with 'S'
		sb.append('S');

		// revision
		int revision = Integer.parseInt(new String(hex, 0, 2), 16);
		sb.append('-');
		sb.append(revision);

		// get count
		int count = Integer.parseInt(new String(hex, 2, 2), 16);

		// check length
		if (bytes.length != (8 + count * 4)) {
			return null;
		}

		// get authority, big-endian
		long authority = Long.parseLong(new String(hex, 4, 12), 16);
		sb.append('-');
		sb.append(authority);

		// sub-authorities, little-endian
		for (int i = 0; i < count; i++) {
			StringBuffer rid = new StringBuffer();
			for (int k = 3; k >= 0; k--) {
				rid.append(hex[16 + (i * 8) + (k * 2)]);
				rid.append(hex[16 + (i * 8) + (k * 2) + 1]);
			}

			long subAuthority = Long.parseLong(rid.toString(), 16);
			sb.append('-');
			sb.append(subAuthority);
		}

		if (sb.toString().trim().isEmpty()) {
			return null;
		}

		return sb.toString();
	}
}
