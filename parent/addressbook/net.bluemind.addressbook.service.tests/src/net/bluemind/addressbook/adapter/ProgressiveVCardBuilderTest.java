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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import net.fortuna.ical4j.vcard.Property.Id;
import net.fortuna.ical4j.vcard.VCard;

public class ProgressiveVCardBuilderTest {

	@Test
	public void testParseOneVCard() throws Exception {
		int count = 0;
		List<VCard> cards = new ArrayList<>();
		try (ProgressiveVCardBuilder builder = new ProgressiveVCardBuilder(cardReader("/tomGmailContacts.vcf"))) {
			while (builder.hasNext()) {
				VCard next = builder.next();
				Assert.assertNotNull(next);
				cards.add(next);
				count++;
			}
		}

		String fn1 = "testbluemind number1";
		String fn2 = "testrobert arthur";
		String fn3 = "testrobin des bois";

		assertTrue(fnFound(fn1, cards));
		assertTrue(fnFound(fn2, cards));
		assertTrue(fnFound(fn3, cards));

		assertEquals(3, count);
	}

	@Test
	public void testParse() throws Exception {
		VCard card = null;
		try (ProgressiveVCardBuilder builder = new ProgressiveVCardBuilder(cardReader("/bitfire.vcf"))) {
			card = builder.next();
		}

		assertNotNull(card);
		assertEquals("01948F59-2796-4184-BFF8-A4F06D21728C", card.getProperties(Id.UID).getFirst().getValue());
	}

	private boolean fnFound(String fn, List<VCard> cards) {
		return cards.stream().filter(card -> card.getProperty(Id.FN).getValue().equals(fn)).count() > 0;
	}

	private Reader cardReader(String name) {
		return new BufferedReader(new InputStreamReader(ProgressiveVCardBuilderTest.class.getResourceAsStream(name)));
	}
}
