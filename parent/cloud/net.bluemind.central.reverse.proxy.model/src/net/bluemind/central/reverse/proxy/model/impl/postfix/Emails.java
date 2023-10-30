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
package net.bluemind.central.reverse.proxy.model.impl.postfix;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import net.bluemind.central.reverse.proxy.model.common.DirInfo.DirEmail;
import net.bluemind.central.reverse.proxy.model.impl.postfix.Domains.DomainAliases;

public class Emails {
	public record EmailUid(String uid, Email email) {
	}

	public record Email(EmailParts parts, boolean allAliases) {
		public boolean match(EmailParts emailParts, DomainAliases domainAliases) {
			if (!parts.left.equals(emailParts.left)) {
				return false;
			}

			if (allAliases) {
				return emailParts.domain.equals(domainAliases.uid())
						|| domainAliases.aliases().contains(emailParts.domain);
			}

			return parts.domain.equals(emailParts.domain);
		}

		public String getEmail() {
			return parts.email();
		}

		public static Email fromDirEmail(DirEmail dirEmail) {
			return EmailParts.fromEmail(dirEmail.address)
					.map(emailParts -> new Email(emailParts, dirEmail.allAliases)).orElse(null);
		}
	}

	public record EmailParts(String left, String domain) {
		public String email() {
			return left + "@" + domain;
		}

		public static Optional<EmailParts> fromEmail(String email) {
			String[] emailParts = email.split("@");
			if (emailParts.length != 2) {
				return Optional.empty();
			}

			return Optional.of(new EmailParts(emailParts[0], emailParts[1]));
		}
	}

	private Set<EmailUid> emails = new HashSet<>();

	public Optional<EmailUid> getEmail(EmailParts emailParts, DomainAliases da) {
		return emails.stream().filter(e -> e.email.match(emailParts, da)).findAny();
	}

	public Optional<EmailUid> getEmailByUid(String uid) {
		return emails.stream().filter(emailUid -> emailUid.uid.equals(uid)).findAny();
	}

	public void update(String emailUid, Collection<DirEmail> dirEmails) {
		Set<EmailUid> emailsUid = dirEmails.stream().map(Email::fromDirEmail).filter(Objects::nonNull)
				.map(e -> new EmailUid(emailUid, e)).collect(Collectors.toSet());

		emails.addAll(emailsUid);
		emails.removeIf(e -> e.uid.equals(emailUid) && !emailsUid.contains(e));
	}

	public void remove(String uid) {
		emails.removeIf(email -> email.uid.equals(uid));
	}
}
