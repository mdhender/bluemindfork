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
package net.bluemind.mailbox.service.internal;

import java.util.Collection;
import java.util.Map;

import net.bluemind.core.api.Email;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.email.EmailHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Type;

public class MailboxSanitizer {
	private ItemValue<Domain> domain;

	public MailboxSanitizer(ItemValue<Domain> domain) {
		this.domain = domain;
	}

	public void sanitize(Mailbox mailbox) throws ServerFault {
		if (!isNullOrEmpty(mailbox.emails)) {
			mailbox.emails = EmailHelper.sanitize(mailbox.emails);
		}

		sanitizeQuota(mailbox);
	}

	private void sanitizeQuota(Mailbox mailbox) {
		if (mailbox.type != Type.mailshare || mailbox.quota != null) {
			return;
		}

		Map<String, String> domainSettings = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomainSettings.class, domain.uid).get();

		mailbox.quota = MailboxQuotaHelper
				.getDefaultQuota(domainSettings, DomainSettingsKeys.mailbox_max_publicfolder_quota.name(),
						DomainSettingsKeys.mailbox_default_publicfolder_quota.name())
				.orElse(null);
	}

	private boolean isNullOrEmpty(Collection<Email> c) {
		return (c == null || c.isEmpty());
	}
}
