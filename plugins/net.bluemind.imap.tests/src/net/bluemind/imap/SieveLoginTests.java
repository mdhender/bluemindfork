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
package net.bluemind.imap;

public class SieveLoginTests extends SieveTestCase {

	public void testB64Decode() {
		String value = "dGhvbWFzQHp6LmNvbQB0aG9tYXNAenouY29tAGFsaWFjb20=";
		byte[] data = java.util.Base64.getDecoder().decode(value);
		System.out.println("Decoded: " + new String(data) + " len: " + data.length);
		for (byte b : data) {
			System.out.println(
					"byte: 0x" + Integer.toHexString(b) + (b > 0 ? " string: " + new String(new byte[] { b }) : ""));
		}

		value = "dGhvbWFzAHRob21hcwBhbGlhY29t";
		data = java.util.Base64.getDecoder().decode(value);
		System.out.println("Decoded: " + new String(data) + " len: " + data.length);
		for (byte b : data) {
			System.out.println(
					"byte: 0x" + Integer.toHexString(b) + (b > 0 ? " string: " + new String(new byte[] { b }) : ""));
		}

	}

}
