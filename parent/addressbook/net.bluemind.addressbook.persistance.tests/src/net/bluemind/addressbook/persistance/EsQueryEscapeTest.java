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
package net.bluemind.addressbook.persistance;

import org.junit.Assert;
import org.junit.Test;

public class EsQueryEscapeTest {

	@Test
	public void testEscapeNothing() throws Exception {
		String query = "";
		String escaped = new VCardIndexStore(null, null).escape(query);

		Assert.assertEquals(query, escaped);
	}

	@Test
	public void testNothingToEscape() throws Exception {
		String query = "hello";
		String escaped = new VCardIndexStore(null, null).escape(query);

		Assert.assertEquals(query, escaped);
	}

	@Test
	public void testSimpleEscape() throws Exception {
		String query = "[test]";
		String escaped = new VCardIndexStore(null, null).escape(query);

		Assert.assertEquals("\\[test\\]", escaped);
	}

	@Test
	public void testMultipleEscapes() throws Exception {
		String query = "[te?st]";
		String escaped = new VCardIndexStore(null, null).escape(query);

		Assert.assertEquals("\\[te\\?st\\]", escaped);
	}

	@Test
	public void testColonDoesNotGetEscaped() throws Exception {
		String query = "[test]:value";
		String escaped = new VCardIndexStore(null, null).escape(query);

		Assert.assertEquals("\\[test\\]:value", escaped);
	}

	@Test
	public void testColonDoesNotGetDetectedAsAlreadyEscaped() throws Exception {
		String query = "[te\\:st]";
		String escaped = new VCardIndexStore(null, null).escape(query);

		Assert.assertEquals("\\[te\\:st\\]", escaped);
	}

	@Test
	public void testAlreadyEscapedStringGetsEscapedAnyway() throws Exception {
		String query = "\\[test\\]";
		String escaped = new VCardIndexStore(null, null).escape(query);

		Assert.assertEquals("\\\\\\[test\\\\\\]", escaped);
	}

	@Test
	public void testColonEscapedAndOtherEscapes() throws Exception {
		String query = "test:[v]a\\:ue";
		String escaped = new VCardIndexStore(null, null).escape(query);

		Assert.assertEquals("test:\\[v\\]a\\:ue", escaped);
	}

}
