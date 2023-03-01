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

import java.util.Optional;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.hook.DomainHookAdapter;
import net.bluemind.openid.api.OpenIdProperties;
import net.bluemind.openid.utils.AccessTokenValidator;

public class OpenIdConfigurationDomainHook extends DomainHookAdapter {

	@Override
	public void onUpdated(BmContext context, ItemValue<Domain> previousValue, ItemValue<Domain> domain)
			throws ServerFault {
		String oldHost = Optional.ofNullable(previousValue.value.properties.get(OpenIdProperties.OPENID_HOST.name()))
				.orElse("");
		String newHost = Optional.ofNullable(domain.value.properties.get(OpenIdProperties.OPENID_HOST.name())).orElse("");

		String oldClient = Optional.ofNullable(previousValue.value.properties.get(OpenIdProperties.OPENID_CLIENT_ID.name()))
				.orElse("");
		String newClient = Optional.ofNullable(domain.value.properties.get(OpenIdProperties.OPENID_CLIENT_ID.name()))
				.orElse("");

		String oldSecret = Optional
				.ofNullable(previousValue.value.properties.get(OpenIdProperties.OPENID_CLIENT_SECRET.name())).orElse("");
		String newSecret = Optional.ofNullable(domain.value.properties.get(OpenIdProperties.OPENID_CLIENT_SECRET.name()))
				.orElse("");

		String oldRealm = Optional.ofNullable(previousValue.value.properties.get(OpenIdProperties.OPENID_REALM.name()))
				.orElse("");
		String newRealm = Optional.ofNullable(domain.value.properties.get(OpenIdProperties.OPENID_REALM.name())).orElse("");

		if (!oldHost.equals(newHost) || !oldClient.equals(newClient) || !oldSecret.equals(newSecret)
				|| !oldRealm.equals(newRealm)) {
			AccessTokenValidator.invalidateCache();
		}
	}

}
