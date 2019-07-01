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
package net.bluemind.exchange.mapi.service;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.exchange.mapi.api.IMapiMailboxes;
import net.bluemind.exchange.mapi.service.internal.MapiMailboxesService;

public class MapiMailboxesServiceFactory
		implements ServerSideServiceProvider.IServerSideServiceFactory<IMapiMailboxes> {

	@Override
	public Class<IMapiMailboxes> factoryClass() {
		return IMapiMailboxes.class;
	}

	private IMapiMailboxes getService(BmContext context, String domainUid) throws ServerFault {
		MapiMailboxesService service = new MapiMailboxesService(context, domainUid);
		return service;
	}

	@Override
	public IMapiMailboxes instance(BmContext context, String... params) throws ServerFault {
		if (params == null || params.length < 1) {
			throw new ServerFault("wrong number of instance parameters");
		}
		String domain = params[0];
		return getService(context, domain);
	}

}
