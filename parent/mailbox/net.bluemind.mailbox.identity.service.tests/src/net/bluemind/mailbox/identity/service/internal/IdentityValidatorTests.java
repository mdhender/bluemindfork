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
package net.bluemind.mailbox.identity.service.internal;

import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.api.Email;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.identity.api.Identity;
import net.bluemind.mailbox.identity.api.SignatureFormat;

public class IdentityValidatorTests {

	private IdentityValidator validator;

	@Before
	public void setup() {
		Mailbox mbox = new Mailbox();
		mbox.emails = Arrays.asList(Email.create("test@checkthat.com", true, false), Email.create("alias", true, true),
				Email.create("base@bm.lan", true, false));
		HashSet<String> aliases = new HashSet<>(Arrays.asList("bm.es", "caramail.com"));
		validator = new IdentityValidator(mbox, aliases, "bm.lan");
	}

	@Test
	public void testValidate() {
		Identity i = defaultIdentity();
		validateNotFail("check nominal", i);

		i = defaultIdentity();
		i.email = "alias@caramail.com";
		validateNotFail("check alias", i);

		i = defaultIdentity();
		i.email = "alias@gg.com";
		validateFail("check alias", i);

		i = defaultIdentity();
		i.name = "";
		validateFail("check name", i);

		i = defaultIdentity();
		i.format = null;
		validateFail("check format", i);

		i = defaultIdentity();
		i.signature = null;
		validateFail("check signature", i);
	}

	private void validateNotFail(String message, Identity identity) {
		try {
			validator.validate(identity);
		} catch (ServerFault e) {
			fail("should not fail (" + message + ") : " + e.getMessage());
		}
	}

	private void validateFail(String message, Identity identity) {
		try {
			validator.validate(identity);
			fail("should fail (" + message + ")");
		} catch (ServerFault e) {

		}
	}

	private Identity defaultIdentity() {
		Identity i = new Identity();
		i.displayname = "test";
		i.name = "test";
		i.email = "base@bm.lan";
		i.format = SignatureFormat.HTML;
		i.signature = "-- gg";
		return i;
	}
}
