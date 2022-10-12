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

import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import com.google.common.base.Strings;

import net.bluemind.core.api.ParametersValidator;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.validator.IValidator;
import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.mailbox.api.MailFilter.Forwarding;
import net.bluemind.mailbox.api.MailFilter.Vacation;
import net.bluemind.mailbox.api.rules.MailFilterRule;
import net.bluemind.mailbox.api.rules.actions.MailFilterRuleActionCopy;
import net.bluemind.mailbox.api.rules.actions.MailFilterRuleActionMove;

public class MailFilterValidator implements IValidator<MailFilter> {

	// FIXME move EMAIL, emailPattern to somewhere accessible everywhere (and
	// reuse it
	// @ net.bluemind.mailbox.persistence.EmailHelper
	private static final String EMAIL = "^[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@([a-z0-9-]+\\.)+[a-z]{2,}$";
	public static final Pattern emailPattern = Pattern.compile(EMAIL);

	public MailFilterValidator() {
	}

	@Override
	public void create(MailFilter obj) throws ServerFault {
		ParametersValidator.notNull(obj);
		ParametersValidator.notNull(obj.rules);
		validateForwarding(obj.forwarding);
		validateVacation(obj.vacation);
		validateRules(obj.rules);
	}

	@Override
	public void update(MailFilter current, MailFilter obj) throws ServerFault {
		ParametersValidator.notNull(obj);
		ParametersValidator.notNull(obj.rules);
		validateForwarding(obj.forwarding);
		validateVacation(obj.vacation);
		validateRules(obj.rules);
	}

	private void validateRules(List<MailFilterRule> rules) throws ServerFault {
		rules.forEach(this::validateRule);
	}

	private void validateRule(MailFilterRule rule) throws ServerFault {
		ParametersValidator.notNull(rule);
		if (!rule.active) {
			return;
		}

		rule.conditions.stream() //
				.flatMap(condition -> condition.filterStream()) //
				.flatMap(filter -> filter.fields.stream()) //
				.filter(field -> field.startsWith("headers")) //
				.map(field -> field.replace("headers.", "")) //
				.filter(headerName -> !headerName.chars().allMatch(c -> c > 31 && c != ' ' && c < 127 && c != ':')) //
				.findFirst() //
				.ifPresent(headerName -> {
					throw new ServerFault("header name " + headerName + " contains invalid characters",
							ErrorCode.INVALID_PARAMETER);
				});

		rule.redirect().ifPresent(redirect -> validateEmailList(redirect.emails()));
		rule.transfer().ifPresent(transfer -> validateEmailList(transfer.emails()));

		if (!rule.hasAction()) {
			throw new ServerFault("No action for the rule", ErrorCode.INVALID_PARAMETER);
		}
		if (hasEmptyDestinationFolder(rule)) {
			throw new ServerFault("Move and copy actions require a destination folder", ErrorCode.INVALID_PARAMETER);
		}
	}

	private boolean hasEmptyDestinationFolder(MailFilterRule rule) {
		return Strings.isNullOrEmpty(rule.move().map(MailFilterRuleActionMove::folder).orElse("Move"))
				|| Strings.isNullOrEmpty(rule.copy().map(MailFilterRuleActionCopy::folder).orElse("Copy"));
	}

	private void validateEmailList(Collection<String> emails) throws ServerFault {
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

		if (vacation.start != null && vacation.end != null && vacation.start.after(vacation.end)) {
			throw new ServerFault("Vacation end date is before its start date", ErrorCode.INVALID_PARAMETER);
		}

		ParametersValidator.notNullAndNotEmpty(vacation.subject);
	}

	private void validateForwarding(Forwarding forwarding) throws ServerFault {
		if (forwarding == null || !forwarding.enabled) {
			return;
		}

		if (forwarding.emails.isEmpty()) {
			throw new ServerFault("Try to activate forwarding but email lists is empty", ErrorCode.INVALID_PARAMETER);
		}
		validateEmailList(forwarding.emails);
	}

}
