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
package net.bluemind.mailbox.service.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Test;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.domain.api.Domain;
import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.mailbox.api.MailFilter.Rule;
import net.bluemind.mailbox.service.internal.MailFilterForwardRoleValidator;
import net.bluemind.role.api.BasicRoles;

public class MailFilterForwardRoleValidatorTest {

	private SecurityContext userContextWithForwarding = new SecurityContext(null, "user", Arrays.<String>asList(),
			Arrays.asList(BasicRoles.ROLE_MAIL_FORWARDING), "test");

	private SecurityContext userContextWithoutForwarding = new SecurityContext(null, "user", Arrays.<String>asList(),
			Arrays.asList("fakeRole"), "test");

	@Test
	public void testValidateForward() {
		MailFilter filter = new MailFilter();
		filter.forwarding = new MailFilter.Forwarding();

		filter.forwarding.enabled = true;
		filter.forwarding.emails = new HashSet<>(Arrays.asList("checkthat@gmail.com"));
		checkOk(SecurityContext.SYSTEM, filter);
		checkOk(userContextWithForwarding, filter);

		// not right to enable forwarding
		checkFail(userContextWithoutForwarding, filter, ErrorCode.FORBIDDEN);
		checkFail(userContextWithoutForwarding, filter, ErrorCode.FORBIDDEN);

		filter.forwarding.emails = new HashSet<>(Arrays.asList("test@dom.lan"));
		checkOk(userContextWithoutForwarding, filter);

	}

	@Test
	public void testValidateRules() {
		MailFilter filter = new MailFilter();

		Rule rule = new MailFilter.Rule();
		rule.active = true;
		rule.forward.emails.add("test@gmail.com");
		filter.rules = Arrays.asList(rule);
		checkOk(SecurityContext.SYSTEM, filter);
		checkOk(userContextWithForwarding, filter);
		// not right to enable forwarding
		checkFail(userContextWithoutForwarding, filter, ErrorCode.FORBIDDEN);

		rule = new MailFilter.Rule();
		rule.active = true;
		rule.forward.emails.add("test@dom.lan");
		filter.rules = Arrays.asList(rule);
		checkOk(userContextWithoutForwarding, filter);
	}

	private void checkOk(SecurityContext sc, MailFilter filter) {
		Domain domain = new Domain();
		ItemValue<Domain> domainItem = ItemValue.create("dom.lan", domain);
		MailFilterForwardRoleValidator validator = new MailFilterForwardRoleValidator(new BmTestContext(sc, null),
				domainItem);

		try {
			validator.create(filter);
		} catch (ServerFault e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	private void checkFail(SecurityContext sc, MailFilter filter, ErrorCode expectErrorCode) {
		Domain domain = new Domain();
		ItemValue<Domain> domainItem = ItemValue.create("dom.lan", domain);
		MailFilterForwardRoleValidator validator = new MailFilterForwardRoleValidator(new BmTestContext(sc, null),
				domainItem);

		try {
			validator.create(filter);
			fail("except throw exception with error code " + expectErrorCode);
		} catch (ServerFault e) {
			assertEquals(expectErrorCode, e.getCode());
		}
	}
}
