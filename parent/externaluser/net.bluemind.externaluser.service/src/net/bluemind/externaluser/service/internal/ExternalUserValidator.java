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

import net.bluemind.core.api.ParametersValidator;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.email.EmailHelper;
import net.bluemind.core.rest.BmContext;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.externaluser.api.ExternalUser;

public class ExternalUserValidator {

	public void validate(ExternalUser eu, String externalUserUid, String domainUid, BmContext bmContext)
			throws ServerFault {
		ParametersValidator.notNull(eu);
		ParametersValidator.notNullAndNotEmpty(eu.defaultEmailAddress());
		ParametersValidator.notNullAndNotEmpty(eu.contactInfos.defaultMail());
		if ((eu.contactInfos.communications.emails.size() < 1) || (eu.emails.size() != 1)) {
			throw new ServerFault("Invalid parameter, an external user should have at least one email.",
					ErrorCode.INVALID_PARAMETER);
		}
		EmailHelper.validate(eu.emails);
		ParametersValidator.notNullAndNotEmpty(eu.contactInfos.identification.formatedName.value);
		ParametersValidator.notNullAndNotEmpty(eu.dataLocation);

		DirEntry dirEntry = bmContext.provider().instance(IDirectory.class, domainUid)
				.getByEmail(eu.defaultEmailAddress());
		if (dirEntry != null && !dirEntry.entryUid.equals(externalUserUid)) {
			throw new ServerFault(
					"Can't create external user: An entry with the same email address already exists in this domain",
					ErrorCode.ALREADY_EXISTS);
		}
	}
}
