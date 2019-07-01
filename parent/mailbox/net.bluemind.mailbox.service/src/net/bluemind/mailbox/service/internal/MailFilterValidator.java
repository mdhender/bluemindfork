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
package net.bluemind.mailbox.service.internal;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.common.base.Strings;

import net.bluemind.core.api.ParametersValidator;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.validator.IValidator;
import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.mailbox.api.MailFilter.Forwarding;
import net.bluemind.mailbox.api.MailFilter.Rule;
import net.bluemind.mailbox.api.MailFilter.Vacation;

public class MailFilterValidator implements IValidator<MailFilter> {

	// FIXME move EMAIL, emailPattern to somewhere accessible everywhere (and
	// reuse it
	// @ net.bluemind.mailbox.persistance.EmailHelper
	private static final String EMAIL = "^[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@([a-z0-9-]+\\.)+[a-z]{2,}$";
	public static final Pattern emailPattern = Pattern.compile(EMAIL);

	private BmContext context;

	public MailFilterValidator(BmContext context) {
		this.context = context;
	}

	@Override
	public void create(MailFilter obj) throws ServerFault {
		ParametersValidator.notNull(obj);
		ParametersValidator.notNull(obj.rules);
		validateForwarding(obj.forwarding);

		validateVacation(obj.vacation);

		validateRules(obj.rules);
	}

	private void validateRules(List<Rule> rules) throws ServerFault {
		for (Rule rule : rules) {
			validateRule(rule);
		}
	}

	private void validateRule(Rule rule) throws ServerFault {
		ParametersValidator.notNull(rule);
		if (!rule.active) {
			return;
		}

		ParametersValidator.nullOrNotEmpty(rule.criteria);

		if (rule.criteria != null) {
			String[] splittedCriteria = rule.criteria.split(":");
			if (splittedCriteria.length > 1) {
				// first param is a HEADER
				String headerName = splittedCriteria[0];
				// 1*<any CHAR, excluding CTLs, SPACE, and ":">
				if (!headerName.chars().allMatch(c -> c > 31 && c != ' ' && c < 127 && c != ':')) {
					throw new ServerFault("header name " + headerName + " contains invalid characters",
							ErrorCode.INVALID_PARAMETER);
				}
			}
		}

		if (!Strings.isNullOrEmpty(rule.deliver)) {
			// TODO validate rule.deliver
		}

		if (!rule.forward.emails.isEmpty()) {
			validateEmailList(rule.forward.emails);
		}

		if (!rule.delete && !rule.discard && !rule.read && !rule.star && rule.forward.emails.isEmpty()
				&& Strings.isNullOrEmpty(rule.deliver)) {
			throw new ServerFault("no action for the rule", ErrorCode.INVALID_PARAMETER);
		}
	}

	private void validateEmailList(Set<String> emails) throws ServerFault {
		for (String e : emails) {
			if (e == null || e.isEmpty()) {
				throw new ServerFault("Null or empty email address", ErrorCode.INVALID_PARAMETER);
			}

			if (!emailPattern.matcher(e).matches()) {
				throw new ServerFault("Invalid email address: " + e, ErrorCode.INVALID_PARAMETER);
			}
		}
	}

	private void validateVacation(Vacation vacation) throws ServerFault {
		if (vacation == null || !vacation.enabled) {
			return;
		}

		ParametersValidator.notNull(vacation.start);
		if (vacation.end != null && new BmDateTimeWrapper(vacation.start).isAfter(vacation.end)) {
			throw new ServerFault("end date is before start date of vacation", ErrorCode.INVALID_PARAMETER);
		}

		ParametersValidator.notNullAndNotEmpty(vacation.subject);
	}

	private void validateForwarding(Forwarding forwarding) throws ServerFault {
		if (forwarding == null) {
			return;
		}

		if (forwarding.enabled) {
			if (forwarding.emails.isEmpty()) {
				throw new ServerFault("Try to activate forward but email lists is empty", ErrorCode.INVALID_PARAMETER);
			}
			validateEmailList(forwarding.emails);
		}
	}

	@Override
	public void update(MailFilter current, MailFilter obj) throws ServerFault {
		ParametersValidator.notNull(obj);
		ParametersValidator.notNull(obj.rules);
		validateForwarding(obj.forwarding);

		validateVacation(obj.vacation);

		validateRules(obj.rules);
	}

}
