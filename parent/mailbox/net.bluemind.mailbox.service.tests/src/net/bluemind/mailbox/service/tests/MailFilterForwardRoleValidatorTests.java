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

import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.api.Email;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.domain.api.Domain;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.mailbox.api.MailFilter.Rule;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.service.internal.MailFilterForwardRoleValidator;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class MailFilterForwardRoleValidatorTests {

	private SecurityContext userContextWithForwarding;

	private SecurityContext userContextWithoutForwarding;

	private ItemValue<Domain> domainItem;
	private static final String DOMAIN_UID = "dom.lan";
	private String mailboxUid;

	@Before
	public void init() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		JdbcTestHelper.getInstance().getDbSchemaService().initialize();

		Domain domain = new Domain();
		domain.name = DOMAIN_UID;
		domainItem = ItemValue.create(DOMAIN_UID, domain);

		PopulateHelper.initGlobalVirt();

		PopulateHelper.createTestDomain(DOMAIN_UID);

		userContextWithForwarding = new SecurityContext("session_user_forwarding", "user", Arrays.<String>asList(),
				Arrays.asList(BasicRoles.ROLE_MAIL_FORWARDING), "test");

		userContextWithoutForwarding = new SecurityContext("session_user_no_forwarding", "user",
				Arrays.<String>asList(), Arrays.asList("fakeRole"), "test");

		createMailbox();
	}

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

		filter.forwarding.emails = new HashSet<>(Arrays.asList("user@dom.lan"));
		checkFail(userContextWithForwarding, filter, ErrorCode.FORBIDDEN);

		filter.forwarding.emails = new HashSet<>(Arrays.asList("toto@dom.lan"));
		checkOk(userContextWithForwarding, filter);
	}

	@Test
	public void testValidateRules() {
		MailFilter filter = new MailFilter();
		Rule rule = new MailFilter.Rule();
		rule.active = true;

		rule.forward.emails = new HashSet<>(Arrays.asList("test@gmail.com"));
		filter.rules = Arrays.asList(rule);
		checkOk(SecurityContext.SYSTEM, filter);
		checkOk(userContextWithForwarding, filter);
		// not right to enable forwarding
		checkFail(userContextWithoutForwarding, filter, ErrorCode.FORBIDDEN);

		rule.forward.emails = new HashSet<>(Arrays.asList("test@dom.lan"));
		filter.rules = Arrays.asList(rule);
		checkOk(userContextWithoutForwarding, filter);

		rule.forward.emails = new HashSet<>(Arrays.asList("user@dom.lan"));
		checkFail(userContextWithForwarding, filter, ErrorCode.FORBIDDEN);

		rule.forward.emails = new HashSet<>(Arrays.asList("toto@dom.lan"));
		checkOk(userContextWithForwarding, filter);
	}

	private void checkOk(SecurityContext sc, MailFilter filter) {
		try {
			MailFilterForwardRoleValidator validator = createValidator(sc, mailboxUid);
			validator.create(filter);
		} catch (ServerFault e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	private void checkFail(SecurityContext sc, MailFilter filter, ErrorCode expectErrorCode) {
		try {
			MailFilterForwardRoleValidator validator = createValidator(sc, mailboxUid);
			validator.create(filter);
			fail("except throw exception with error code " + expectErrorCode);
		} catch (ServerFault e) {
			assertEquals(expectErrorCode, e.getCode());
		}
	}

	private void createMailbox() {
		BmTestContext bmTestContext = new BmTestContext(SecurityContext.SYSTEM);

		IMailboxes mailboxesService = bmTestContext.provider().instance(IMailboxes.class, DOMAIN_UID);
		ItemValue<Mailbox> mailbox = mailboxesService.list().get(0);
		Email email = new Email();
		email.address = "user@dom.lan";

		mailboxUid = mailbox.uid.concat("_test");
		mailbox.uid = mailboxUid;
		mailbox.value.name = mailbox.value.name.concat("_test");
		mailbox.value.emails = Arrays.asList(email);
		mailboxesService.create(mailboxUid, mailbox.value);
	}

	private MailFilterForwardRoleValidator createValidator(SecurityContext sc, String mailboxUid) {
		return new MailFilterForwardRoleValidator(new BmTestContext(sc), domainItem, mailboxUid);
	}
}
