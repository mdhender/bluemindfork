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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.smime.cacerts.service;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.smime.cacerts.api.ISmimeCRL;
import net.bluemind.smime.cacerts.service.internal.SmimeCRLService;

public class SmimeCRLServiceFactory implements ServerSideServiceProvider.IServerSideServiceFactory<ISmimeCRL> {

	@Override
	public Class<ISmimeCRL> factoryClass() {
		return ISmimeCRL.class;
	}

	@Override
	public ISmimeCRL instance(BmContext context, String... params) throws ServerFault {
		if (params == null || params.length < 1) {
			throw new ServerFault("wrong number of instance parameters");
		}
		RBACManager.forContext(context).checkNotAnoynmous();
		return new SmimeCRLService(context, params[0]);
	}

}
