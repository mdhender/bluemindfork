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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.base.Strings;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.validator.IValidator;
import net.bluemind.domain.api.Domain;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.mailbox.api.MailFilter.Forwarding;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.rules.MailFilterRule;
import net.bluemind.role.api.BasicRoles;

public class MailFilterForwardRoleValidator implements IValidator<MailFilter> {

	private ItemValue<Domain> domain;
	private BmContext context;
	private String mailboxUid;

	public MailFilterForwardRoleValidator(BmContext context, ItemValue<Domain> domain, String mailboxUid) {
		this.domain = domain;
		this.context = context;
		this.mailboxUid = mailboxUid;
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

			if (!forwarding.emails.isEmpty() && !Strings.isNullOrEmpty(mailboxUid)) {
				checkUserEmailsNotUsed(forwarding.emails);
			}
		}

	}

	private boolean hasExternal(Collection<String> emails) {
		return emails.stream().map(email -> {
			String[] parts = email.split("@");
			return parts.length == 2 && !domain.uid.equals(parts[1]) && !domain.value.aliases.contains(parts[1]);
		}).reduce(false, (a, b) -> a || b);
	}

	private void validateRules(List<MailFilterRule> rules) {
		rules.stream().filter(rule -> rule.active).forEach(this::validateRule);
	}

	private void validateRule(MailFilterRule rule) throws ServerFault {
		rule.redirect().ifPresent(redirect -> {
			if (!redirect.emails().isEmpty()) {
				if (hasExternal(redirect.emails())) {
					checkCanSetExternalForward();
				}
				if (!Strings.isNullOrEmpty(mailboxUid)) {
					checkUserEmailsNotUsed(redirect.emails());
				}
			}
		});
	}

	private void checkCanSetExternalForward() {
		if (!context.getSecurityContext().isDomainAdmin(context.getSecurityContext().getContainerUid())
				&& !context.getSecurityContext().getRoles().contains(BasicRoles.ROLE_MAIL_FORWARDING)) {
			throw new ServerFault("no right to enable forwarding ", ErrorCode.FORBIDDEN);
		}
	}

	private void checkUserEmailsNotUsed(Collection<String> emails) {
		ItemValue<Mailbox> mailbox = context.su().provider().instance(IMailboxes.class, domain.uid)
				.getComplete(mailboxUid);

		List<String> invalidEmails = emails.stream()
				.flatMap(email -> mailbox.value.emails.stream().filter(em -> em.address.equals(email)))
				.map(Object::toString).toList();
		if (!invalidEmails.isEmpty()) {
			throw new ServerFault("Forwarding to own user emails is not authorized: "
					+ invalidEmails.stream().collect(Collectors.joining(",")), ErrorCode.FORBIDDEN);
		}
	}
}
