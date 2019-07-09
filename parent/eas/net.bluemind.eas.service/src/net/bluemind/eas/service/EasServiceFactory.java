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
package net.bluemind.eas.service;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.eas.api.IEas;
import net.bluemind.eas.service.internal.EasService;

public class EasServiceFactory implements ServerSideServiceProvider.IServerSideServiceFactory<IEas> {

	@Override
	public Class<IEas> factoryClass() {
		return IEas.class;
	}

	@Override
	public IEas instance(BmContext context, String... params) throws ServerFault {
		return new EasService(context);
	}

}
