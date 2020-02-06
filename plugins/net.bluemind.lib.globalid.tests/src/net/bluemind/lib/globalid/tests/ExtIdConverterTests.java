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
package net.bluemind.lib.globalid.tests;

import java.util.Base64;

import junit.framework.TestCase;
import net.bluemind.lib.globalid.ExtIdConverter;

public class ExtIdConverterTests extends TestCase {

	private void checkGOIDToExtId(String goid, String extId) {
		String converted = ExtIdConverter.toExtId(goid);
		System.out.println("converted: " + converted);
		System.out.println("expected : " + extId);
		assertEquals(extId, converted);
	}

	private void checkExtToGlobal(String goid, String extId) {
		String converted = ExtIdConverter.fromExtId(extId);
		System.out.println("converted: " + converted);
		System.out.println("expected : " + goid);
		assertEquals(goid, converted);
	}

	public void testGlobalToExt() {
		checkGOIDToExtId(
				"040000008200E00074C5B7101A82E0080000000000000000000000000000000000000000350000007643616C2D556964010000003730643264373731353863636664306435313564393261623165313932656437373465653565633500",
				"70d2d77158ccfd0d515d92ab1e192ed774ee5ec5");
	}

	public void testExtToGlobal() {
		checkExtToGlobal(
				"040000008200E00074C5B7101A82E0080000000000000000000000000000000000000000350000007643616C2D556964010000003730643264373731353863636664306435313564393261623165313932656437373465653565633500",
				"70d2d77158ccfd0d515d92ab1e192ed774ee5ec5");

		checkExtToGlobal(
				"040000008200E00074C5B7101A82E0080000000000000000000000000000000000000000350000007643616C2D556964010000006163366639366335623032346533653738656364373236626334656531643233633731373666336600",
				"ac6f96c5b024e3e78ecd726bc4ee1d23c7176f3f");
	}

	public void testIsThirdParty() {
		String gaid = "040000008200E00074C5B7101A82E0080000000000000000000000000000000000000000350000007643616C2D556964010000003730643264373731353863636664306435313564393261623165313932656437373465653565633500";
		boolean thirdParty = ExtIdConverter.isThirdPartyGlobalId(gaid);
		assertTrue(thirdParty);
	}

	public void testASBlogValues() {
		// values are from
		// http://blogs.msdn.com/b/exchangedev/archive/2011/07/22/working-with-meeting-requests-in-exchange-activesync.aspx
		String uid = "A3561BDAAE8E4B30AC255FD3F31A3AD700000000000000000000000000000000";
		String b64globalId = "BAAAAIIA4AB0xbcQGoLgCAAAAAAAAAAAAAAAAAAAAAAAAAAATQAAAHZDYWwtVWlkAQAAAEEzNTYxQkRBQUU4RTRCMzBBQzI1NUZEM0YzMUEzQUQ3MDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAA";
		byte[] origBytes = fromB64(b64globalId);
		String goId = ExtIdConverter.toHexString(origBytes);
		System.out.println("un-b64: '" + goId + "'");
		System.out.println("orig uid: " + uid);
		String calc = ExtIdConverter.toExtId(goId);
		System.out.println("calc uid: " + calc);
		assertEquals(calc, uid);

		byte[] calculatedBytes = ExtIdConverter.fromHexString(goId);
		assertEquals(origBytes.length, calculatedBytes.length);
		for (int i = 0; i < origBytes.length; i++) {
			assertEquals(origBytes[i], calculatedBytes[i]);
		}
	}

	private static byte[] fromB64(String s) {
		return Base64.getDecoder().decode(s);
	}

}
