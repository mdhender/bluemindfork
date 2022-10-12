/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.mailbox.service.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.mailbox.api.MailFilter.Forwarding;
import net.bluemind.mailbox.api.rules.MailFilterRule;

public class MailFilterSanitizerTest {
	private static final MailFilterSanitizer sanitizer = new MailFilterSanitizer();

	@Test
	public void create_lowercaseForwardingEmailsList() {
		MailFilter mf = new MailFilter();
		mf.forwarding = new Forwarding();
		mf.forwarding.emails.add("DAVID@BM.LAN");

		sanitizer.create(mf);

		assertEquals("david@bm.lan", mf.forwarding.emails.iterator().next());
	}

	@Test
	public void create_nullForwarding() {
		MailFilter mf = new MailFilter();
		mf.forwarding = null;

		sanitizer.create(mf);
		assertNotNull(mf.forwarding);
	}

	@Test
	public void create_nullForwardingEmailsList() {
		MailFilter mf = new MailFilter();
		mf.forwarding.emails = null;

		sanitizer.create(mf);
		assertNotNull(mf.forwarding.emails);
		assertTrue(mf.forwarding.emails.isEmpty());
	}

	@Test
	public void create_nullVacation() {
		MailFilter mf = new MailFilter();
		mf.vacation = null;

		sanitizer.create(mf);
		assertNotNull(mf.vacation);
	}

	@Test
	public void create_nullRules() {
		MailFilter mf = new MailFilter();
		mf.rules = null;

		sanitizer.create(mf);
		assertNotNull(mf.rules);
		assertTrue(mf.rules.isEmpty());
	}

	@Test
	public void create_ruleEmptyDeliver() {
		MailFilterRule r = new MailFilterRule();
		r.addMove("");

		MailFilter mf = new MailFilter();
		mf.rules = Arrays.asList(r);

		sanitizer.create(mf);
		assertNull(mf.rules.get(0).move().orElse(null));
	}
}
