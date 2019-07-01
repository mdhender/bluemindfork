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
package net.bluemind.eas.serdes.settings;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.eas.dto.NamespaceMapping;
import net.bluemind.eas.dto.base.Callback;
import net.bluemind.eas.dto.settings.OofState;
import net.bluemind.eas.dto.settings.SettingsResponse;
import net.bluemind.eas.serdes.IEasResponseFormatter;
import net.bluemind.eas.serdes.IResponseBuilder;

public class SettingsResponseFormatter implements IEasResponseFormatter<SettingsResponse> {

	private static final Logger logger = LoggerFactory.getLogger(SettingsResponseFormatter.class);

	@Override
	public void format(IResponseBuilder builder, double protocolVersion, SettingsResponse sr,
			Callback<Void> completion) {
		if (logger.isDebugEnabled()) {
			logger.debug("Formatting {}", sr);
		}
		builder.start(NamespaceMapping.Settings);
		builder.text("Status", sr.status.xmlValue());

		if (sr.userInformation != null) {
			builder.container("UserInformation");
			builder.text("Status", "1");
			builder.container("Get");

			if (protocolVersion > 14.0) {
				builder.container("Accounts");
				builder.container("Account");
			}

			builder.container("EmailAddresses");
			Set<String> mails = sr.userInformation.smtpAddresses;
			for (String email : mails) {
				if (!email.equals(sr.userInformation.primaryAddress)) {
					builder.text("SMTPAddress", email);
				}
			}

			// The PrimarySmtpAddress element is not supported when the MS-
			// ASProtocolVersion header value is set to 12.1 or 14.0
			if (protocolVersion > 14.0) {
				builder.text("PrimarySmtpAddress", sr.userInformation.primaryAddress);
			}

			builder.endContainer(); // Get

			if (protocolVersion > 14.0) {
				builder.endContainer(); // Accounts
				builder.endContainer(); // Account
			}

			builder.endContainer(); // Get EmailAddresses
			builder.endContainer(); // UserInformation
		}

		if (sr.deviceInformation != null) {
			builder.container("DeviceInformation");
			builder.text("Status", "1");
			builder.endContainer();
		}

		if (sr.oof != null) {
			builder.container("Oof");
			builder.text("Status", "1");
			builder.container("Get");
			builder.text("OofState", OofState.disabled.xmlValue());
			builder.endContainer().endContainer();
		}
		builder.end(completion);
	}

}
