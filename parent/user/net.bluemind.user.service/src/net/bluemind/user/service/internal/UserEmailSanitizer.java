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

import java.util.ArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;

import net.bluemind.core.api.Email;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sanitizer.ISanitizer;
import net.bluemind.core.sanitizer.ISanitizerFactory;
import net.bluemind.directory.service.DirDomainValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.user.api.User;

public class UserEmailSanitizer implements ISanitizer<DirDomainValue<User>> {

	public static final class Factory implements ISanitizerFactory<DirDomainValue<User>> {

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
	public void create(DirDomainValue<User> obj) throws ServerFault {
		if (!(obj.value instanceof User)) {
			return;
		}
		sanitizeEmails(obj.domainUid, obj.value);
	}

	@Override
	public void update(DirDomainValue<User> current, DirDomainValue<User> obj) throws ServerFault {
		if (!(obj.value instanceof User)) {
			return;
		}
		sanitizeEmails(obj.domainUid, obj.value);
	}

	private void sanitizeEmails(String domainName, User user) {
		if (user.routing != Routing.internal) {
			return;
		}

		String defaultAlias = getDomainDefaultAlias(domainName);

		if (domainName.endsWith(".internal")) {
			// clean eventual old_login@.internal
			user.emails = user.emails.stream().filter(e -> {
				String[] parts = e.address.split("@");
				return !(parts[1].equals(domainName) && !parts[0].equals(user.login));
			}).collect(Collectors.toList());
		}

		sanitizeEmail(user, defaultAlias, false);
		if (domainName.endsWith(".internal")) {
			sanitizeEmail(user, domainName, true);
		}

	}

	protected String getDomainDefaultAlias(String domainName) {
		Domain domain = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IDomains.class)
				.get(domainName).value;
		String defaultAlias = domain.defaultAlias;
		return defaultAlias;
	}

	private void sanitizeEmail(User user, String domainPart, boolean explicit) {
		Function<Boolean, Boolean> allAliasesCheck = (allAliases) -> !explicit && allAliases;
		if (user.emails.stream().filter(e -> {
			String[] parts = e.address.split("@");
			return parts[0].equals(user.login)
					&& (allAliasesCheck.apply(e.allAliases) || (parts.length == 2 && parts[1].equals(domainPart)));
		}).count() == 0) {
			user.emails = new ArrayList<Email>(user.emails);
			user.emails.add(Email.create(user.login + "@" + domainPart, false, false));
		}
	}

}
