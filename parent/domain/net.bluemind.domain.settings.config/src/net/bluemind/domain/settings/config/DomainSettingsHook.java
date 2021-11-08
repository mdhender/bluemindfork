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

		Optional<String> previousExternal = Optional.empty();
		Optional<String> currentExternal = Optional.empty();
		Optional<String> previousDefaultDomain = Optional.empty();
		Optional<String> currentDefaultDomain = Optional.empty();

		if (previousSettings != null && !previousSettings.isEmpty()) {
			previousExternal = getExternalUrlSetting(previousSettings);
			currentExternal = getExternalUrlSetting(currentSettings);
			externalUrlSettingsChange = !(previousExternal.equals(currentExternal));

			previousDefaultDomain = getDefaultDomainSetting(previousSettings);
			currentDefaultDomain = getDefaultDomainSetting(currentSettings);
			defaultDomainSettingsChange = !(previousDefaultDomain.equals(currentDefaultDomain));
		}

		if (externalUrlSettingsChange || defaultDomainSettingsChange) {
			JsonObject payload = new JsonObject();
			payload.put("domainUid", domain.uid);

			if (externalUrlSettingsChange) {
				payload.put("externalUrlUpdated", externalUrlSettingsChange)
						.put("externalUrlOld", previousExternal.orElse(null))
						.put("externalUrlNew", currentExternal.orElse(null));
			}

			if (defaultDomainSettingsChange) {
				payload.put("defaultDomainUpdated", defaultDomainSettingsChange)
						.put("defaultDomainOld", previousDefaultDomain.orElse(null))
						.put("defaultDomainNew", currentDefaultDomain.orElse(null));
			}

			VertxPlatform.eventBus().publish("domainsettings.config.updated", payload);
		}

	}

	private static Optional<String> getDefaultDomainSetting(Map<String, String> settings) {
		return Optional.ofNullable(settings.get(DomainSettingsKeys.default_domain.name()));
	}

	private static Optional<String> getExternalUrlSetting(Map<String, String> settings) {
		return Optional.ofNullable(settings.get(DomainSettingsKeys.external_url.name()));
	}

}
