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
package net.bluemind.addressbook.adapter;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;

import org.junit.Assert;
import org.junit.Test;

public class ProgressiveVCardBuilderTest {

	@Test
	public void testParseOneVCard() throws Exception {
		int count = 0;
		try (ProgressiveVCardBuilder builder = new ProgressiveVCardBuilder(cardReader("/tomGmailContacts.vcf"))) {
			while (builder.hasNext()) {
				Assert.assertNotNull(builder.next());
				count++;
			}
		}
		assertEquals(3, count);
	}

	private Reader cardReader(String name) {
		return new BufferedReader(new InputStreamReader(ProgressiveVCardBuilderTest.class.getResourceAsStream(name)));
	}
}
