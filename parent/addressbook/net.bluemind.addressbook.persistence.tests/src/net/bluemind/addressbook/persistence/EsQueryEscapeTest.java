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
package net.bluemind.addressbook.persistence;

import org.junit.Assert;
import org.junit.Test;

import net.bluemind.addressbook.api.VCardQuery;

public class EsQueryEscapeTest {

	@Test
	public void testEscapeNothing() throws Exception {
		VCardQuery query = VCardQuery.create("", true);
		String escaped = VCardIndexStore.escape(query);

		Assert.assertEquals(query.query, escaped);
	}

	@Test
	public void testNothingToEscape() throws Exception {
		VCardQuery query = VCardQuery.create("hello", true);
		String escaped = VCardIndexStore.escape(query);

		Assert.assertEquals(query.query, escaped);
	}

	@Test
	public void testSimpleEscape() throws Exception {
		VCardQuery query = VCardQuery.create("[test]", true);
		String escaped = VCardIndexStore.escape(query);

		Assert.assertEquals("\\[test\\]", escaped);
	}

	@Test
	public void testMultipleEscapes() throws Exception {
		VCardQuery query = VCardQuery.create("[te?st]", true);
		String escaped = VCardIndexStore.escape(query);

		Assert.assertEquals("\\[te\\?st\\]", escaped);
	}

	@Test
	public void testColonDoesNotGetEscaped() throws Exception {
		VCardQuery query = VCardQuery.create("[test]:value", true);
		String escaped = VCardIndexStore.escape(query);

		Assert.assertEquals("\\[test\\]:value", escaped);
	}

	@Test
	public void testColonDoesNotGetDetectedAsAlreadyEscaped() throws Exception {
		VCardQuery query = VCardQuery.create("[te\\:st]", true);
		String escaped = VCardIndexStore.escape(query);

		Assert.assertEquals("\\[te\\:st\\]", escaped);
	}

	@Test
	public void testAlreadyEscapedStringGetsEscapedAnyway() throws Exception {
		VCardQuery query = VCardQuery.create("\\[test\\]", true);
		String escaped = VCardIndexStore.escape(query);

		Assert.assertEquals("\\\\\\[test\\\\\\]", escaped);
	}

	@Test
	public void testColonEscapedAndOtherEscapes() throws Exception {
		VCardQuery query = VCardQuery.create("test:[v]a\\:ue", true);
		String escaped = VCardIndexStore.escape(query);

		Assert.assertEquals("test:\\[v\\]a\\:ue", escaped);
	}

}
