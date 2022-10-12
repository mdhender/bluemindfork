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

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;

import org.junit.Test;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.core.validator.IValidator;
import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.mailbox.api.MailFilter.Vacation;
import net.bluemind.mailbox.api.rules.MailFilterRule;
import net.bluemind.mailbox.api.rules.conditions.MailFilterRuleCondition;
import net.bluemind.mailbox.service.internal.MailFilterValidatorFactory;

public class MailFilterValidatorTest {

	@Test
	public void testValidateNullParams() {
		MailFilter filter = null;
		// filter null
		checkFail(SecurityContext.SYSTEM, filter, ErrorCode.INVALID_PARAMETER);
		filter = new MailFilter();
		filter.rules = null;
		// rules null
		checkFail(SecurityContext.SYSTEM, filter, ErrorCode.INVALID_PARAMETER);
	}

	@Test
	public void testValidateForward() {
		MailFilter filter = new MailFilter();
		filter.forwarding = new MailFilter.Forwarding();

		filter.forwarding.enabled = true;
		filter.forwarding.emails = new HashSet<>(Arrays.asList("checkthat@gmail.com"));
		checkOk(SecurityContext.SYSTEM, filter);
		filter.forwarding.emails = new HashSet<>(Arrays.asList("bademail.com"));
		checkFail(SecurityContext.SYSTEM, filter, ErrorCode.INVALID_PARAMETER);

		// empty list
		filter.forwarding.emails = new HashSet<>();
		checkFail(SecurityContext.SYSTEM, filter, ErrorCode.INVALID_PARAMETER);
	}

	@Test
	public void testInvalidateComposedFilter() {
		MailFilter filter = new MailFilter();
		MailFilterRule rule = new MailFilterRule();
		rule.conditions.add(MailFilterRuleCondition.equal("from", "toto@yahoo.fr"));
		rule.conditions.add(MailFilterRuleCondition.equal("headers.bad header", "fdss"));
		rule.addMarkAsRead();
		filter.rules = Arrays.asList(rule);

		checkFail(SecurityContext.SYSTEM, filter, ErrorCode.INVALID_PARAMETER);
	}

	@Test
	public void testValidateRules() {
		MailFilter filter = new MailFilter();
		MailFilterRule rule = new MailFilterRule();
		rule.active = false;
		filter.rules = Arrays.asList(rule);
		checkOk(SecurityContext.SYSTEM, filter);

		rule = new MailFilterRule();
		filter.rules = Arrays.asList(rule);
		// no actions
		checkFail(SecurityContext.SYSTEM, filter, ErrorCode.INVALID_PARAMETER);

		rule = new MailFilterRule();
		rule.addRedirect(Arrays.asList("test.cm"), false);
		filter.rules = Arrays.asList(rule);
		// invalid forward email
		checkFail(SecurityContext.SYSTEM, filter, ErrorCode.INVALID_PARAMETER);

		rule = new MailFilterRule();
		rule.addMarkAsDeleted();
		filter.rules = Arrays.asList(rule);
		checkOk(SecurityContext.SYSTEM, filter);

		rule = new MailFilterRule();
		rule.addDiscard();
		filter.rules = Arrays.asList(rule);
		checkOk(SecurityContext.SYSTEM, filter);

		rule = new MailFilterRule();
		rule.addMarkAsRead();
		filter.rules = Arrays.asList(rule);
		checkOk(SecurityContext.SYSTEM, filter);

		rule = new MailFilterRule();
		rule.addMarkAsImportant();
		filter.rules = Arrays.asList(rule);
		checkOk(SecurityContext.SYSTEM, filter);

		rule = new MailFilterRule();
		rule.addMove("");
		filter.rules = Arrays.asList(rule);
		// invalid destination folder
		checkFail(SecurityContext.SYSTEM, filter, ErrorCode.INVALID_PARAMETER);

		rule = new MailFilterRule();
		rule.addMove("sent");
		filter.rules = Arrays.asList(rule);
		checkOk(SecurityContext.SYSTEM, filter);

		rule = new MailFilterRule();
		rule.conditions.add(MailFilterRuleCondition.equal("from", "poeut"));
		rule.addMove("sent");
		filter.rules = Arrays.asList(rule);
		checkOk(SecurityContext.SYSTEM, filter);

		rule = new MailFilterRule();
		rule.conditions.add(MailFilterRuleCondition.equal("headers.My-magicHeader$", "poeut"));
		rule.addMove("sent");
		filter.rules = Arrays.asList(rule);
		checkOk(SecurityContext.SYSTEM, filter);

		rule = new MailFilterRule();
		rule.conditions.add(MailFilterRuleCondition.equal("headers.My-magicHeader not good$", "poeut"));
		rule.addMove("sent");
		filter.rules = Arrays.asList(rule);
		checkFail(SecurityContext.SYSTEM, filter, ErrorCode.INVALID_PARAMETER);

	}

	@Test
	public void testValidateVacation() {
		MailFilter filter = new MailFilter();
		filter.vacation = new MailFilter.Vacation();

		filter.vacation.enabled = false;
		checkOk(SecurityContext.SYSTEM, filter);

		filter.vacation = fullVacation();
		checkOk(SecurityContext.SYSTEM, filter);

		filter.vacation = fullVacation();
		filter.vacation.end = null;
		// end date with null value is valid
		checkOk(SecurityContext.SYSTEM, filter);

		filter.vacation = fullVacation();
		filter.vacation.start = null;
		// begin date with null value is not valid
		checkOk(SecurityContext.SYSTEM, filter);

		filter.vacation = fullVacation();
		filter.vacation.start = Date.from(LocalDate.of(2020, 01, 02).atStartOfDay(ZoneId.of("UTC")).toInstant());
		filter.vacation.end = Date.from(LocalDate.of(2020, 01, 01).atStartOfDay(ZoneId.of("UTC")).toInstant());
		// begin date after end date is not valid
		checkFail(SecurityContext.SYSTEM, filter, ErrorCode.INVALID_PARAMETER);

		filter.vacation = fullVacation();
		filter.vacation.subject = "";
		checkFail(SecurityContext.SYSTEM, filter, ErrorCode.INVALID_PARAMETER);

		filter.vacation = fullVacation();
		filter.vacation.text = "";
		checkOk(SecurityContext.SYSTEM, filter);

	}

	private Vacation fullVacation() {
		Vacation vacation = new MailFilter.Vacation();
		vacation.enabled = true;
		vacation.start = Date.from(LocalDate.of(2020, 01, 01).atStartOfDay(ZoneId.of("UTC")).toInstant());
		vacation.end = Date.from(LocalDate.of(2020, 01, 02).atStartOfDay(ZoneId.of("UTC")).toInstant());
		vacation.subject = "toto";
		vacation.text = "toto";
		return vacation;
	}

	private void checkOk(SecurityContext sc, MailFilter filter) {
		IValidator<MailFilter> validator = new MailFilterValidatorFactory().create(new BmTestContext(sc, null));
		try {
			validator.create(filter);
		} catch (ServerFault e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	private void checkFail(SecurityContext sc, MailFilter filter, ErrorCode expectErrorCode) {
		IValidator<MailFilter> validator = new MailFilterValidatorFactory().create(new BmTestContext(sc, null));
		try {
			validator.create(filter);
			fail("except throw exception with error code " + expectErrorCode);
		} catch (ServerFault e) {
			assertEquals(expectErrorCode, e.getCode());
		}
	}
}
