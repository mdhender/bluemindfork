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
package net.bluemind.resource.service;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.core.api.Email;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.resource.api.ResourceDescriptor;
import net.bluemind.resource.service.internal.ResourceVCardAdapter;

public class ResourceVCardAdapterTest {

	@Test
	public void adatp() {
		ResourceDescriptor rd = new ResourceDescriptor();
		rd.label = "test";
		rd.emails = Arrays.asList(Email.create("bla@domain.com", true));
		rd.description = "el description";

		Domain domain = Domain.create("domain.com", "domain.com", "dodo", Collections.emptySet());
		VCard card = new ResourceVCardAdapter().asVCard(ItemValue.create("domain.com", domain), "test", rd);
		assertEquals("test", card.identification.formatedName.value);
		assertEquals("test", card.identification.name.familyNames);
		assertEquals(1, card.communications.emails.size());
		assertEquals("bla@domain.com", card.communications.emails.get(0).value);
		assertEquals("el description", card.explanatory.note);
	}
}
