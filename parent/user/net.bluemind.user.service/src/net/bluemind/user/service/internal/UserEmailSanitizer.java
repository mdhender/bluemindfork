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
package net.bluemind.user.service.internal;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import net.bluemind.core.api.Email;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sanitizer.ISanitizer;
import net.bluemind.core.sanitizer.ISanitizerFactory;
import net.bluemind.directory.service.DirDomainValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.user.api.User;

public class UserEmailSanitizer implements ISanitizer<DirDomainValue<User>> {
	public static final class Factory implements ISanitizerFactory<DirDomainValue<User>> {
		@SuppressWarnings("unchecked")
		@Override
		public Class<DirDomainValue<User>> support() {
			return (Class<DirDomainValue<User>>) ((Class<?>) DirDomainValue.class);
		}

		@Override
		public ISanitizer<DirDomainValue<User>> create(BmContext context) {
			return new UserEmailSanitizer();
		}

	}

	@Override
	public void create(DirDomainValue<User> userDirDomain) throws ServerFault {
		if (!(userDirDomain.value instanceof User)) {
			return;
		}
		sanitizeEmails(userDirDomain.domainUid, userDirDomain.entryUid, userDirDomain.value);
	}

	@Override
	public void update(DirDomainValue<User> current, DirDomainValue<User> userDirDomain) throws ServerFault {
		if (!(userDirDomain.value instanceof User)) {
			return;
		}
		sanitizeEmails(userDirDomain.domainUid, userDirDomain.entryUid, userDirDomain.value);
	}

	private void sanitizeEmails(String domainUid, String userUid, User user) {
		if (user.routing != Routing.internal) {
			return;
		}

		Domain domain = getDomainAliases(domainUid);

		if (!domain.defaultAlias.equals(domainUid) && !domain.aliases.contains(domainUid)) {
			// if domainUid is not a domain alias or the default domain
			// remove all @domainUid email except login@domainUid
			user.emails = user.emails.stream().filter(e -> {
				String[] parts = e.address.split("@");
				return !(!e.allAliases && parts[1].equals(domainUid) && !parts[0].equals(user.login));
			}).collect(Collectors.toList());
		}

		// Ensure login@domainUid exists
		if (!user.emails.stream().anyMatch(e -> isImplicitEmail(domainUid, user, e))) {
			user.emails = new HashSet<>(user.emails);
			user.emails.add(Email.create(user.login + "@" + domainUid, false, false));

			if (isUserRename(domainUid, userUid, user)) {
				user.emails.add(Email.create(user.login + "@" + domain.defaultAlias, false, false));
			}
		}

		// Default email
		if (!user.emails.stream().anyMatch(e -> e.isDefault)) {
			user.emails = setDefaultEmail(domain, user);
		}
	}

	private boolean isImplicitEmail(String domainUid, User user, Email email) {
		String[] parts = email.address.split("@");
		return user.login.equals(parts[0]) && (email.allAliases || (parts.length == 2 && domainUid.equals(parts[1])));
	}

	private Collection<Email> setDefaultEmail(Domain domain, User user) {
		Set<Email> emails = user.emails.stream().map(e -> setDefaultAsDefault(domain, e, Optional.of(user.login)))
				.collect(Collectors.toSet());
		if (emails.stream().anyMatch(e -> e.isDefault)) {
			return emails;
		}

		emails = user.emails.stream().map(e -> setDefaultAsDefault(domain, e, Optional.empty()))
				.collect(Collectors.toSet());
		if (!emails.stream().anyMatch(e -> e.isDefault)) {
			emails.add(Email.create(user.login + "@" + domain.defaultAlias, true, false));
		}

		return emails;
	}

	/**
	 * Set email as default email if :
	 * <ul>
	 * <li>if user.login is present: email.leftpart == user.login and (is allAliases
	 * or email.domainpart is a domain alias)</li>
	 *
	 * <li>else: is allAliases or email.domainpart is a domain alias</li>
	 * </ul>
	 * 
	 * @param domain
	 * @param user
	 * @param email
	 * @return
	 */
	private Email setDefaultAsDefault(Domain domain, Email email, Optional<String> userLogin) {
		String[] parts = email.address.split("@");
		if (userLogin.map(ul -> ul.equals(parts[0])).orElse(true) && (email.allAliases || (parts.length == 2
				&& (domain.aliases.contains(parts[1]) || domain.defaultAlias.equals(parts[1]))))) {
			return Email.create(email.address, true, email.allAliases);
		}

		return email;
	}

	protected boolean isUserRename(String domainUid, String userUid, User user) {
		ItemValue<Mailbox> mailbox = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IMailboxes.class, domainUid).getComplete(userUid);
		if (mailbox == null) {
			return false;
		}

		if (mailbox.value.name.equals(user.login)) {
			return false;
		}

		return true;
	}

	protected Domain getDomainAliases(String domainUid) {
		return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IDomains.class)
				.get(domainUid).value;
	}
}
