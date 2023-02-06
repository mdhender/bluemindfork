/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.openid.configuration.internal;

import java.util.Map;
import java.util.Optional;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.domain.hook.DomainHookAdapter;
import net.bluemind.openid.utils.AccessTokenValidator;

public class OpenIdConfigurationDomainHook extends DomainHookAdapter {

	@Override
	public void onSettingsUpdated(BmContext context, ItemValue<Domain> domain, Map<String, String> previousSettings,
			Map<String, String> currentSettings) throws ServerFault {
		String prev = Optional.ofNullable(previousSettings.get(DomainSettingsKeys.openid_host.name())).orElse("");
		String now = Optional.ofNullable(currentSettings.get(DomainSettingsKeys.openid_host.name())).orElse("");
		if (!now.equals(prev)) {
			AccessTokenValidator.invalidateCache();
		}

	}

}
