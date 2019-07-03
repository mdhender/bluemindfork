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
package net.bluemind.user.service.internal;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.api.Email;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.identity.api.SignatureFormat;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.user.api.UserMailIdentity;

public class UserMailIdentityValidatorTests {

	@Before
	public void setup() {

	}

	private UserMailIdentityValidator createValidator(SecurityContext context) {
		Mailbox mbox = new Mailbox();
		mbox.emails = Arrays.asList(Email.create("test@checkthat.com", true, false), Email.create("alias", true, true),
				Email.create("base@bm.lan", true, false));

		Map<String, ItemValue<Mailbox>> mboxes = new HashMap<>();
		ItemValue<Mailbox> mboxValue = new ItemValue<>();
		mboxValue.value = mbox;
		mboxValue.uid = "test";
		mboxes.put("test", mboxValue);

		mbox = new Mailbox();
		mbox.emails = Arrays.asList(Email.create("iboul@checkthat.com", true, false), Email.create("ialop", true, true),
				Email.create("ouka@bm.lan", true, false));

		mboxValue = new ItemValue<>();
		mboxValue.value = mbox;
		mboxValue.uid = "iboul";

		mboxes.put("iboul", mboxValue);
		FakeMailboxes fakeMailboxes = new FakeMailboxes(mboxes);

		HashSet<String> aliases = new HashSet<>(Arrays.asList("bm.es", "caramail.com"));

		return new UserMailIdentityValidator(fakeMailboxes, "bm.lan", aliases, context);
	}

	@Test
	public void testValidate() {
		String domainUid = "dom" + System.currentTimeMillis() + ".test";
		String sid = "sid" + System.currentTimeMillis();
		SecurityContext context = new SecurityContext(sid, "admin@" + domainUid, new ArrayList<String>(),
				Arrays.asList(SecurityContext.ROLE_ADMIN), domainUid);
		UserMailIdentityValidator validator = createValidator(context);

		UserMailIdentity i = defaultIdentity();
		validateNotFail("check nominal", i, validator);

		i = defaultIdentity();
		i.email = "alias@caramail.com";
		validateNotFail("check alias", i, validator);

		i = defaultIdentity();
		i.email = "alias@gg.com";
		validateFail("check alias", i, validator);

		i = defaultIdentity();
		i.name = "";
		validateFail("check name", i, validator);

		i = defaultIdentity();
		i.format = null;
		validateFail("check format", i, validator);

		i = defaultIdentity();
		i.signature = null;
		validateFail("check signature", i, validator);

		i = defaultIdentity();
		i.mailboxUid = "badMailboxUid";
		validateFail("check non-existant mailbox", i, validator);

		// Test external identity
		context = new SecurityContext(sid, "admin@" + domainUid, new ArrayList<String>(),
				Arrays.asList(SecurityContext.ROLE_ADMIN, BasicRoles.ROLE_EXTERNAL_IDENTITY), domainUid);

		validator = createValidator(context);

		i = defaultIdentity();
		i.email = "batman@blue-mind.net";
		// identity which targets an external email has no associated mailbox
		i.mailboxUid = null;
		validateNotFail("check external identity", i, validator);

		i = defaultIdentity();
		i.email = "iboul@checkthat.com";
		i.mailboxUid = "test";
		validateFail("check identity theft with external role", i, validator);

	}

	private void validateNotFail(String message, UserMailIdentity identity, UserMailIdentityValidator validator) {
		try {
			validator.validate(identity);
		} catch (ServerFault e) {
			fail("should not fail (" + message + ") : " + e.getMessage());
		}
	}

	private void validateFail(String message, UserMailIdentity identity, UserMailIdentityValidator validator) {
		try {
			validator.validate(identity);
			fail("should fail (" + message + ")");
		} catch (ServerFault e) {

		}
	}

	private UserMailIdentity defaultIdentity() {
		UserMailIdentity i = new UserMailIdentity();
		i.displayname = "test";
		i.name = "test";
		i.email = "base@bm.lan";
		i.format = SignatureFormat.HTML;
		i.signature = "-- gg";
		i.mailboxUid = "test";
		i.sentFolder = "Sent";
		return i;
	}

}
