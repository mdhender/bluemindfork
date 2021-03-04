/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import net.bluemind.domain.api.Domain;
import net.bluemind.user.api.User;

public class TestUserEmailSanitizer extends UserEmailSanitizer {
	private final String defaultAlias;
	private final Set<String> aliases;

	public TestUserEmailSanitizer() {
		this("domain.tld");
	}

	public TestUserEmailSanitizer(String defaultAlias) {
		this(defaultAlias, new HashSet<String>(Arrays.asList(defaultAlias)));
	}

	public TestUserEmailSanitizer(String defaultAlias, Set<String> aliases) {
		this.defaultAlias = defaultAlias;
		this.aliases = aliases;
	}

	@Override
	protected Domain getDomainAliases(String domainUid) {
		return Domain.create(domainUid, "domain label", "domain description", aliases, defaultAlias);
	}

	@Override
	protected boolean isUserRename(String domainUid, String userUid, User user) {
		return user.login.contains("isrename");
	}
}
