/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.node.client.tests;

import static org.junit.Assert.assertEquals;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import org.junit.Test;

import net.bluemind.node.client.NodePathEscaper;

public class EscapeTest {

	@Test
	public void testEscapeUnescape() throws UnsupportedEncodingException {
		String s = "/var/spool/cyrus/meta/2297b864-eacc-481b-9671-5742860c853b__jardport_com/domain/j/jardport.com/f/user/f^maot/k+n";
		String escaped = new NodePathEscaper().escape(s);
		String unescaped = URLDecoder.decode(escaped, "UTF-8");
		assertEquals(s, unescaped);
	}

}
