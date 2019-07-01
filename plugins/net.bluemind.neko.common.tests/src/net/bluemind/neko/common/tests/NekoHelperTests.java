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
package net.bluemind.neko.common.tests;

import java.io.IOException;
import java.io.InputStream;

import com.google.common.io.ByteStreams;

import junit.framework.TestCase;
import net.bluemind.neko.common.NekoHelper;

public class NekoHelperTests extends TestCase {

	private InputStream open(String n) {
		return NekoHelperTests.class.getClassLoader().getResourceAsStream("data/" + n);
	}

	public void testIdentityRewrite() throws IOException {
		InputStream testData = open("withEntities.html");
		InputStream result = NekoHelper.identityTransform(testData, "UTF-8");
		byte[] data = ByteStreams.toByteArray(result);
		result.close();
		String html = new String(data);
		System.out.println("html:\n" + html);
		assertFalse(html.contains("eacute"));
		assertTrue(html.contains("€"));
	}

}
