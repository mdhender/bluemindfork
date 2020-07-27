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

import java.util.Collections;
import java.util.HashSet;
import java.util.stream.Collectors;

import com.google.common.base.Strings;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.sanitizer.ISanitizer;
import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.mailbox.api.MailFilter.Forwarding;
import net.bluemind.mailbox.api.MailFilter.Vacation;

public class MailFilterSanitizer implements ISanitizer<MailFilter> {

	@Override
	public void create(MailFilter obj) throws ServerFault {
		sanitize(obj);

	}

	@Override
	public void update(MailFilter current, MailFilter obj) throws ServerFault {
		sanitize(obj);
	}

	private void sanitize(MailFilter obj) {
		if (obj == null) {
			return;
		}

		if (obj.forwarding == null) {
			obj.forwarding = new Forwarding();
		} else if (obj.forwarding.emails == null) {
			obj.forwarding.emails = new HashSet<>();
		}
		obj.forwarding.emails = obj.forwarding.emails.stream().map(String::toLowerCase).collect(Collectors.toSet());

		if (obj.vacation == null) {
			obj.vacation = new Vacation();
		}

		if (obj.rules == null) {
			obj.rules = Collections.emptyList();
		}

		obj.rules.forEach(this::sanitizeRule);
	}

	private void sanitizeRule(MailFilter.Rule rule) {
		if (rule.deliver != null && Strings.isNullOrEmpty(rule.deliver)) {
			rule.deliver = null;
		}

		if (rule.forward == null) {
			rule.forward = new Forwarding();
		}
	}

}