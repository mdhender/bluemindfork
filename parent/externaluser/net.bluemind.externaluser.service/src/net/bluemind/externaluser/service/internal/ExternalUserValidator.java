/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.externaluser.service.internal;

import java.sql.SQLException;

import com.google.common.base.Strings;

import net.bluemind.core.api.ParametersValidator;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.email.EmailHelper;
import net.bluemind.core.rest.BmContext;
import net.bluemind.externaluser.api.ExternalUser;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.persistence.MailboxStore;

public class ExternalUserValidator {

	private final MailboxStore mailboxStore;

	public ExternalUserValidator(MailboxStore mailboxStore) {
		this.mailboxStore = mailboxStore;
	}

	public void validate(ExternalUser eu, Long externalUserId, String domainUid, BmContext bmContext)
			throws ServerFault {
		ParametersValidator.notNull(eu);
		ParametersValidator.notNullAndNotEmpty(eu.defaultEmailAddress());
		ParametersValidator.notNullAndNotEmpty(eu.contactInfos.defaultMail());
		if ((eu.contactInfos.communications.emails.isEmpty()) || (eu.emails.size() != 1)) {
			throw new ServerFault("Invalid parameter, an external user should have at least one email.",
					ErrorCode.INVALID_PARAMETER);
		}
		EmailHelper.validate(eu.emails);
		ParametersValidator.notNullAndNotEmpty(eu.contactInfos.identification.formatedName.value);
		ParametersValidator.notNullAndNotEmpty(eu.dataLocation);

		String familyName = eu.contactInfos.identification.name.familyNames;
		if (Strings.isNullOrEmpty(familyName)) {
			throw new ServerFault("An external user should have a last name.", ErrorCode.EMPTY_LASTNAME);
		}

		try {
			if (mailboxStore.emailAlreadyUsed(externalUserId, eu.emails)) {
				throw new ServerFault(
						"Email of external user is already in use: " + eu.emails.iterator().next().address,
						ErrorCode.ALREADY_EXISTS);
			}
		} catch (SQLException sqle) {
			throw ServerFault.sqlFault(sqle);
		}

		ItemValue<Mailbox> mbox = bmContext.provider().instance(IMailboxes.class, domainUid)
				.byEmail(eu.defaultEmailAddress());
		if (mbox != null) {
			throw new ServerFault(
					"Can't create external user: A mailbox with the same email address already exists in this domain",
					ErrorCode.EMAIL_ALREADY_USED);
		}

	}
}
