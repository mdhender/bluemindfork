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
package net.bluemind.domain.service.internal;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox.Routing;

public class DomainSettingsValidator {
	private final Logger logger = LoggerFactory.getLogger(DomainSettingsValidator.class);

	public void create(Map<String, String> settings) throws ServerFault {
		checkSplitDomain(settings);
		checkDomainMaxUsers(settings);
	}

	public void update(Map<String, String> oldSettings, Map<String, String> newSettings, String domain)
			throws ServerFault {
		checkSplitDomain(newSettings);

		if (null != getRelay(oldSettings) && (null == getRelay(newSettings))) {
			List<String> externalUids = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
					.instance(IMailboxes.class, domain).byRouting(Routing.external);
			if (!externalUids.isEmpty()) {
				logger.error("{} Routing.external user(s)", externalUids.size());
				throw new ServerFault("Cannot unset split relay. Some mailboxes are still Routing.external",
						ErrorCode.INVALID_PARAMETER);
			}

		}

		checkDomainMaxUsers(newSettings);
	}

	private void checkSplitDomain(Map<String, String> settings) throws ServerFault {
		if (isForwardUnknownToRelay(settings) && null == getRelay(settings)) {
			throw new ServerFault("Split domain relay hostname cannot be empty", ErrorCode.INVALID_PARAMETER);
		}
	}

	private boolean isForwardUnknownToRelay(Map<String, String> settings) {
		return Boolean.valueOf(settings.get(DomainSettingsKeys.mail_forward_unknown_to_relay.name())).booleanValue();
	}

	private String getRelay(Map<String, String> settings) {
		boolean notNull = settings.get(DomainSettingsKeys.mail_routing_relay.name()) != null;
		boolean empty = true;
		if (notNull) {
			empty = settings.get(DomainSettingsKeys.mail_routing_relay.name()).trim().isEmpty();
		}
		if (!empty) {
			return settings.get(DomainSettingsKeys.mail_routing_relay.name());
		}
		return null;
	}

	private void checkDomainMaxUsers(Map<String, String> settings) throws ServerFault {
		String domainMaxUser = settings.get(DomainSettingsKeys.domain_max_users.name());
		if (domainMaxUser == null || domainMaxUser.isEmpty()) {
			return;
		}

		Integer maxUsers = null;
		try {
			maxUsers = Integer.parseInt(domainMaxUser);
		} catch (NumberFormatException nfe) {
			throw new ServerFault("Invalid maximum number of users. Must be an integer greater than 0.",
					ErrorCode.INVALID_PARAMETER);
		}

		if (maxUsers < 1) {
			throw new ServerFault("Invalid maximum number of users. Must be an integer greater than 0.",
					ErrorCode.INVALID_PARAMETER);
		}
	}
}
