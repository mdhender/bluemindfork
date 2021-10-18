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
package net.bluemind.cti.service;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider.IServerSideServiceFactory;
import net.bluemind.cti.api.IComputerTelephonyIntegration;
import net.bluemind.cti.backend.CTIBackendProvider;
import net.bluemind.cti.service.internal.CTIStatusManager;

public class CTIFactory implements IServerSideServiceFactory<IComputerTelephonyIntegration> {

	private final static CTIStatusManager statusManager = new CTIStatusManager();

	@Override
	public Class<IComputerTelephonyIntegration> factoryClass() {
		return IComputerTelephonyIntegration.class;
	}

	@Override
	public IComputerTelephonyIntegration instance(BmContext context, String... params) throws ServerFault {
		if (params == null || params.length < 2) {
			throw new ServerFault("wrong number of instance parameters");
		}

		String domain = params[0];
		String userUid = params[1];

		return new ComputerTelephonyIntegration(context, statusManager, domain, userUid,
				CTIBackendProvider.getBackend(domain, userUid));
	}

}
