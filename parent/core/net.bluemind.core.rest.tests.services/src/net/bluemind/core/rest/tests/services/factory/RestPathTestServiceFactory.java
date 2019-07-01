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
package net.bluemind.core.rest.tests.services.factory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.rest.tests.services.IRestPathTestService;
import net.bluemind.core.rest.tests.services.RestPathTestServiceImpl;

public class RestPathTestServiceFactory
		implements ServerSideServiceProvider.IServerSideServiceFactory<IRestPathTestService> {

	@Override
	public Class<IRestPathTestService> factoryClass() {
		return IRestPathTestService.class;
	}

	@Override
	public IRestPathTestService instance(BmContext context, String... params) throws ServerFault {

		return new RestPathTestServiceImpl(params[0], params[1]);
	}

}
