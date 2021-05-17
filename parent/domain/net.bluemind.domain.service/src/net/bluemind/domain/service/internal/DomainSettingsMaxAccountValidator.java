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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.domain.service.internal;

import java.util.Map;

import org.apache.commons.lang.StringUtils;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.rest.BmContext;
import net.bluemind.domain.api.DomainSettings;

abstract class DomainSettingsMaxAccountValidator {

	protected void create(BmContext context, DomainSettings settings, String key, String role) throws ServerFault {
		// null is default value, allow it on create
		if (settings.settings.containsKey(key) && settings.settings.get(key) != null) {
			RBACManager.forContext(context).check(role);
			checkValue(settings.settings, key);
		}
	}

	protected void update(BmContext context, DomainSettings oldValue, DomainSettings newValue, String key, String role)
			throws ServerFault {
		if (!StringUtils.equals(oldValue.settings.get(key), newValue.settings.get(key))) {
			RBACManager.forContext(context).check(role);
			checkValue(newValue.settings, key);
		}
	}

	private void checkValue(Map<String, String> settings, String key) throws ServerFault {
		String setting = settings.get(key);
		if (setting == null || setting.isEmpty()) {
			return;
		}

		try {
			Integer.parseInt(setting);
		} catch (NumberFormatException nfe) {
			throw new ServerFault("Invalid maximum number of " + key + " accounts.", ErrorCode.INVALID_PARAMETER);
		}
	}

}
