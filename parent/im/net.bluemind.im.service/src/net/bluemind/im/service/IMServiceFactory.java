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
package net.bluemind.im.service;

import org.elasticsearch.client.Client;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.im.api.IInstantMessaging;
import net.bluemind.im.service.internal.IMService;
import net.bluemind.lib.elasticsearch.ESearchActivator;

public class IMServiceFactory implements ServerSideServiceProvider.IServerSideServiceFactory<IInstantMessaging> {

	public IMServiceFactory() {

	}

	@Override
	public Class<IInstantMessaging> factoryClass() {
		return IInstantMessaging.class;
	}

	@Override
	public IInstantMessaging instance(BmContext context, String... params) throws ServerFault {

		Client es = ESearchActivator.getClient();
		if (es == null) {
			throw new ServerFault("elasticsearch was not found for contact indexing");
		}

		return new IMService(context, es);
	}

}
