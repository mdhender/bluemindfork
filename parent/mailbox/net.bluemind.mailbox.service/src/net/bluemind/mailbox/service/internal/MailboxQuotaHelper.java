/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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

import java.util.Map;
import java.util.Optional;

public class MailboxQuotaHelper {
	public static Optional<Integer> getDefaultQuota(Map<String, String> domainSettings, String maxQuotaKey,
			String defaultQuotaKey) {
		int quota;
		if (domainSettings.containsKey(defaultQuotaKey)
				&& (quota = Integer.parseInt(domainSettings.get(defaultQuotaKey))) != 0) {
			return Optional.of(quota);
		}

		if (domainSettings.containsKey(maxQuotaKey)
				&& (quota = Integer.parseInt(domainSettings.get(maxQuotaKey))) != 0) {
			return Optional.of(quota);
		}

		return Optional.empty();
	}
}
