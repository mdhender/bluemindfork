/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
  *
  * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License)
  * or the CeCILL as published by CeCILL.info (version 2 of the License).
  *
  * There are special exceptions to the terms and conditions of the
  * licenses as they are applied to this program. See LICENSE.txt in
  * the directory of this program distribution.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.backend.mail.replica.service.internal.tools;

import net.bluemind.core.api.Email;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.sendmail.SendmailCredentials;
import net.bluemind.domain.api.Domain;
import net.bluemind.user.api.User;

public class EnvelopFrom {
	private final ItemValue<Domain> domain;

	public EnvelopFrom(ItemValue<Domain> domain) {
		this.domain = domain;
	}

	/**
	 * Return best SMTP envelop <i>from</i> from authenticated user and <i>from</i>
	 * email header
	 * 
	 * Expect to be compliant for SPF/DKIM/DMARC
	 * 
	 * @param domain
	 * @param user
	 * @param creds
	 * @param fromMail
	 * @return
	 */
	public String getFor(SendmailCredentials creds, User user, String fromMail) {
		if (creds.isAdminO()) {
			return fromMail;
		}

		if (fromMail.contains("@") && user.emails.stream().anyMatch(e -> e.match(fromMail, domain.value.aliases))) {
			return fromMail;
		}

		// Search user email with same domain part as fromMail (SPF/DMARC compliance)
		String[] fromMailParts = fromMail.split("@");
		if (fromMailParts.length != 2) {
			// fromMail with no domain parts ?... use user default email
			return user.defaultEmailAddress();
		}

		// Use default email if available in fromMail domain...
		String fromMailDomain = fromMailParts[1];
		if (match(fromMailDomain, user.defaultEmail())) {
			return user.defaultEmail().localPart() + "@" + fromMailDomain;
		}

		// ... or any email available in fromMail domain, otherwise fallback to default
		// email
		return user.emails.stream().filter(e -> match(fromMailDomain, e)).findAny()
				.map(e -> e.localPart() + "@" + fromMailDomain).orElse(user.defaultEmailAddress());
	}

	/**
	 * Match if email is available in <i>domainPart</i> domain
	 * 
	 * @param domainPart
	 * @param email
	 * @return
	 */
	private boolean match(String domainPart, Email email) {
		return (domain.value.aliases.contains(domainPart) && email.allAliases) || email.domainPart().equals(domainPart);
	}
}
