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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.sanitizer.ISanitizer;
import net.bluemind.core.sanitizer.ISanitizerFactory;
import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.mailbox.api.MailFilter.Forwarding;
import net.bluemind.mailbox.api.MailFilter.Vacation;

public class MailFilterSanitizer implements ISanitizer<MailFilter> {
	private static final Logger logger = LoggerFactory.getLogger(MailFilterSanitizer.class);

	public static class Factory implements ISanitizerFactory<MailFilter> {
		@Override
		public Class<MailFilter> support() {
			return MailFilter.class;
		}

		@Override
		public ISanitizer<MailFilter> create(BmContext context, Container container) {
			return new MailFilterSanitizer();
		}
	}

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

		if (obj.rules == null) {
			obj.rules = Collections.emptyList();
		}
		obj.rules = obj.rules.stream() //
				.filter(rule -> rule.move().map(move -> !Strings.isNullOrEmpty(move.folder())).orElse(true)) //
				.map(rule -> {
					rule.transfer().ifPresent(transfer -> transfer.emails = sanitizeEmailList(transfer.emails));
					rule.redirect().ifPresent(redirect -> redirect.emails = sanitizeEmailList(redirect.emails));
					return rule;
				}).toList();

		if (obj.vacation == null) {
			obj.vacation = new Vacation();
		}
	}

	private List<String> sanitizeEmailList(List<String> emails) {
		return (emails == null) //
				? new ArrayList<>()
				: emails.stream().map(String::toLowerCase).toList();
	}

}