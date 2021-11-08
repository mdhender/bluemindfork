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
package net.bluemind.domain.service;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.service.internal.IInCoreDomainSettings;

public class InCoreDomainSettingsServiceFactory extends DomainSettingsCommonFactory
		implements ServerSideServiceProvider.IServerSideServiceFactory<IInCoreDomainSettings> {

	public InCoreDomainSettingsServiceFactory() {
	}

	@Override
	public Class<IInCoreDomainSettings> factoryClass() {
		return IInCoreDomainSettings.class;
	}

	@Override
	public IInCoreDomainSettings instance(BmContext context, String... params) throws ServerFault {
		return instanceImpl(context, params);
	}
}
