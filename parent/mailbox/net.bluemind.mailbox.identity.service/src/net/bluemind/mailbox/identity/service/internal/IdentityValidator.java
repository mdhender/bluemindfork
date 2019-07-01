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
package net.bluemind.mailbox.identity.service.internal;

import java.util.Set;

import net.bluemind.core.api.Email;
import net.bluemind.core.api.ParametersValidator;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.email.EmailHelper;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.identity.api.Identity;

public class IdentityValidator {

	private Mailbox mboxValue;
	private Set<String> domainAliases;
	private String domainUid;

	public IdentityValidator(Mailbox mboxValue, Set<String> domainAliases, String domainUid) {
		this.domainUid = domainUid;
		this.mboxValue = mboxValue;
		this.domainAliases = domainAliases;
	}

	public void validate(Identity identity) throws ServerFault {
		ParametersValidator.notNull(identity);
		ParametersValidator.notNullAndNotEmpty(identity.name);

		ParametersValidator.notNullAndNotEmpty(identity.email);
		EmailHelper.validate(identity.email);

		ParametersValidator.notNull(identity.format);
		ParametersValidator.notNull(identity.signature);

		boolean addressFound = false;
		String[] smail = identity.email.split("@");
		for (Email email : mboxValue.emails) {
			if (!email.allAliases) {
				if (email.address.equals(identity.email)) {
					addressFound = true;
					break;
				}
			} else {
				String adr = email.address;
				if (adr.contains("@")) {
					adr = adr.split("@")[0];
				}
				if (smail[0].equals(adr) && (domainAliases.contains(smail[1]) || smail[1].equals(domainUid))) {
					addressFound = true;
					break;
				}
			}
		}

		if (!addressFound) {
			throw new ServerFault("email " + identity.email + " not found into mailbox emails");
		}
	}
}
