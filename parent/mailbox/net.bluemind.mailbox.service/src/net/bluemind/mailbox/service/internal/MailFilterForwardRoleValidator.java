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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.validator.IValidator;
import net.bluemind.domain.api.Domain;
import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.mailbox.api.MailFilter.Forwarding;
import net.bluemind.mailbox.api.MailFilter.Rule;
import net.bluemind.role.api.BasicRoles;

public class MailFilterForwardRoleValidator implements IValidator<MailFilter> {

	private ItemValue<Domain> domain;
	private BmContext context;

	public MailFilterForwardRoleValidator(BmContext context, ItemValue<Domain> domain) {
		this.domain = domain;
		this.context = context;
	}

	@Override
	public void create(MailFilter obj) throws ServerFault {
		validateForwarding(null, obj.forwarding);
		validateRules(obj.rules);
	}

	@Override
	public void update(MailFilter oldValue, MailFilter newValue) throws ServerFault {
		validateForwarding(oldValue != null ? oldValue.forwarding : null, newValue.forwarding);

		validateRules(newValue.rules);
	}

	private void validateForwarding(Forwarding old, Forwarding forwarding) {

		if (forwarding.enabled) {

			Set<String> diff = new HashSet<>(forwarding.emails);

			if (old != null && old.emails != null) {
				diff.removeAll(old.emails);
			}

			boolean external = hasExternal(forwarding.emails);

			if (external) {
				checkCanSetExternalForward();
			}
		}
	}

	private boolean hasExternal(Set<String> emails) {
		return emails.stream().map(email -> {
			String[] parts = email.split("@");
			return parts.length == 2 && !domain.uid.equals(parts[1]) && !domain.value.aliases.contains(parts[1]);
		}).reduce(false, (a, b) -> a || b);
	}

	private void validateRules(List<Rule> rules) {
		for (Rule rule : rules) {
			validateRule(rule);
		}
	}

	private void validateRule(Rule rule) throws ServerFault {
		if (!rule.active) {
			return;
		}

		if (!rule.forward.emails.isEmpty()) {
			if (hasExternal(rule.forward.emails)) {
				checkCanSetExternalForward();
			}

		}
	}

	private void checkCanSetExternalForward() {
		if (!context.getSecurityContext().isDomainAdmin(context.getSecurityContext().getContainerUid())
				&& !context.getSecurityContext().getRoles().contains(BasicRoles.ROLE_MAIL_FORWARDING)) {
			throw new ServerFault("no right to enable forwarding ", ErrorCode.FORBIDDEN);
		}

	}
}
