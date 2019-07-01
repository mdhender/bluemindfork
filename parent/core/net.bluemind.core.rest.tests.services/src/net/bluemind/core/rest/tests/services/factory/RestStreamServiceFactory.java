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
import net.bluemind.core.rest.tests.services.IRestStreamTestService;
import net.bluemind.core.rest.tests.services.RestStreamImpl;
import net.bluemind.lib.vertx.VertxPlatform;

public class RestStreamServiceFactory
		implements ServerSideServiceProvider.IServerSideServiceFactory<IRestStreamTestService> {

	@Override
	public Class<IRestStreamTestService> factoryClass() {
		return IRestStreamTestService.class;
	}

	@Override
	public IRestStreamTestService instance(BmContext context, String... params) throws ServerFault {
		return new RestStreamImpl(VertxPlatform.getVertx());
	}

}
