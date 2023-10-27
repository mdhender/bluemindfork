/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.utils;

import java.util.List;

import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SysConfKeys;

public class ClientSideTrust extends Trust {

	private final IServiceProvider provider;

	public ClientSideTrust(IServiceProvider provider) {
		this.provider = provider;
	}

	@Override
	protected List<String> getTrustAllModules() {
		return provider.instance(ISystemConfiguration.class).getValues()
				.stringList(SysConfKeys.tls_trust_allcertificates.name());
	}
}
