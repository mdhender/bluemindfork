/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.domain.settings.config;

import java.util.Map;
import java.util.Optional;

import io.vertx.core.json.JsonObject;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.domain.hook.DomainHookAdapter;
import net.bluemind.lib.vertx.VertxPlatform;

public class DomainSettingsHook extends DomainHookAdapter {

	@Override
	public void onSettingsUpdated(BmContext context, ItemValue<Domain> domain, Map<String, String> previousSettings,
			Map<String, String> currentSettings) throws ServerFault {

		boolean externalUrlSettingsChange = true;
		boolean defaultDomainSettingsChange = true;
		if (previousSettings != null && !previousSettings.isEmpty()) {
			externalUrlSettingsChange = externalUrlHasChanged(previousSettings, currentSettings);
			defaultDomainSettingsChange = defaultDomainHasChanged(previousSettings, currentSettings);
		}

		if (externalUrlSettingsChange || defaultDomainSettingsChange) {
			JsonObject payload = new JsonObject();
			payload.put("externalUrlUpdated", externalUrlSettingsChange).put("defaultDomainUpdated",
					defaultDomainSettingsChange);
			VertxPlatform.eventBus().publish("domainsettings.config.file.update", payload);
		}

	}

	private boolean externalUrlHasChanged(Map<String, String> previousSettings, Map<String, String> currentSettings) {

		Optional<String> previousExternal = Optional
				.ofNullable(previousSettings.get(DomainSettingsKeys.external_url.name()));
		Optional<String> currentExternal = Optional
				.ofNullable(currentSettings.get(DomainSettingsKeys.external_url.name()));

		return !(previousExternal.equals(currentExternal));

	}

	private boolean defaultDomainHasChanged(Map<String, String> previousSettings, Map<String, String> currentSettings) {

		Optional<String> previousDefaultDomain = Optional
				.ofNullable(previousSettings.get(DomainSettingsKeys.default_domain.name()));
		Optional<String> currentDefaultDomain = Optional
				.ofNullable(currentSettings.get(DomainSettingsKeys.default_domain.name()));

		return !(previousDefaultDomain.equals(currentDefaultDomain));

	}

}
