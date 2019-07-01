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
package net.bluemind.mailshare.service.internal;

import java.util.Collection;

import net.bluemind.core.api.Email;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.email.EmailHelper;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.validator.IValidator;
import net.bluemind.core.validator.IValidatorFactory;
import net.bluemind.mailshare.api.Mailshare;

public class MailshareValidator implements IValidator<Mailshare> {

	public static class Factory implements IValidatorFactory<Mailshare> {

		@Override
		public Class<Mailshare> support() {
			return Mailshare.class;
		}

		@Override
		public IValidator<Mailshare> create(BmContext context) {
			return new MailshareValidator();
		}

	}

	public void validate(Mailshare mailbox) throws ServerFault {

		if (mailbox == null) {
			throw new ServerFault("Mailshare is null", ErrorCode.INVALID_PARAMETER);
		}

		if (isNullOrEmpty(mailbox.name)) {
			throw new ServerFault("Mailshare.name must be set ", ErrorCode.INVALID_PARAMETER);
		}
		// name should be mailbox-name compatible
		try {
			EmailHelper.validate(mailbox.name + "@test.com");
		} catch (ServerFault e) {
			throw new ServerFault("Mailshare.name (" + mailbox.name + ") is not mailbox compatible",
					ErrorCode.INVALID_PARAMETER);
		}

		if (mailbox.routing == null) {
			throw new ServerFault("Mailshare.routing must be set", ErrorCode.INVALID_PARAMETER);
		}

		if (!isNullOrEmpty(mailbox.emails)) {
			EmailHelper.validate(mailbox.emails);
		}

	}

	private boolean isNullOrEmpty(String s) {
		return (s == null || s.trim().isEmpty());
	}

	private boolean isNullOrEmpty(Collection<Email> c) {
		return (c == null || c.isEmpty());
	}

	@Override
	public void create(Mailshare obj) throws ServerFault {
		validate(obj);
	}

	@Override
	public void update(Mailshare oldValue, Mailshare newValue) throws ServerFault {
		validate(newValue);
	}

}
