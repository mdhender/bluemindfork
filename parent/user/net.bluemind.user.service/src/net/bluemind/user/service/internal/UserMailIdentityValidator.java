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

import java.util.Set;

import net.bluemind.core.api.Email;
import net.bluemind.core.api.ParametersValidator;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.email.EmailHelper;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.user.api.UserMailIdentity;

public class UserMailIdentityValidator {

	private Set<String> domainAliases;
	private IMailboxes mailboxes;
	private SecurityContext securityContext;
	private String domainUid;

	public UserMailIdentityValidator(IMailboxes mailboxes, String domainUid, Set<String> domainAliases,
			SecurityContext securityContext) {
		this.domainUid = domainUid;
		this.mailboxes = mailboxes;
		this.domainAliases = domainAliases;
		this.securityContext = securityContext;
	}

	public void validate(UserMailIdentity identity) throws ServerFault {
		ParametersValidator.notNull(identity);
		ParametersValidator.notNullAndNotEmpty(identity.name);

		ParametersValidator.notNullAndNotEmpty(identity.email);
		EmailHelper.validate(identity.email);

		ParametersValidator.notNull(identity.format);
		ParametersValidator.notNull(identity.signature);
		ParametersValidator.notNull(identity.sentFolder);

		if (identity.mailboxUid != null) {
			ItemValue<Mailbox> mbox = validateMboxUid(identity.mailboxUid);
			boolean addressFound = false;
			String[] smail = identity.email.split("@");
			for (Email email : mbox.value.emails) {
				if (email.allAliases) {
					if (smail[0].equals(email.address.split("@")[0])
							&& (smail[1].equals(domainUid) || domainAliases.contains(smail[1]))) {
						addressFound = true;
						break;
					}
				} else {
					if (email.address.equals(identity.email)) {
						addressFound = true;
						break;
					}
				}
			}
			// The tested context is the connected user one, this is a feature
			// not a
			// bug (an admin who can create external identity can create
			// external
			// identity for another user).
			if (!addressFound) {
				throw new ServerFault(
						"email " + identity.email + " not found in " + identity.mailboxUid + "mailbox emails");
			}
		} else {
			if (!securityContext.getRoles().contains(BasicRoles.ROLE_EXTERNAL_IDENTITY)) {
				throw new ServerFault("user " + securityContext.getSubject()
						+ " does not own permission to manage external identity " + identity.email);
			}
		}

	}

	private ItemValue<Mailbox> validateMboxUid(String mailboxUid) throws ServerFault {
		ItemValue<Mailbox> ret = mailboxes.getComplete(mailboxUid);
		if (ret == null) {
			throw new ServerFault("mailbox " + mailboxUid + " not found", ErrorCode.INVALID_PARAMETER);
		}

		return ret;
	}

	public void beforeDelete(UserMailIdentity identity) {
		if (identity.isDefault) {
			String msg = String.format("Default identity %s cannot be deleted", identity.displayname);
			throw new ServerFault(msg);
		}
	}
}
