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
package net.bluemind.server.hook;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.domain.api.Domain;
import net.bluemind.server.api.Server;

public class DefaultServerHook implements IServerHook {

	@Override
	public void onServerCreated(BmContext context, ItemValue<Server> item) throws ServerFault {

	}

	@Override
	public void onServerUpdated(BmContext context, ItemValue<Server> previousValue, Server value) throws ServerFault {

	}

	@Override
	public void onServerDeleted(BmContext context, ItemValue<Server> itemValue) throws ServerFault {

	}

	@Override
	public void onServerTagged(BmContext context, ItemValue<Server> itemValue, String tag) throws ServerFault {

	}

	@Override
	public void onServerUntagged(BmContext context, ItemValue<Server> itemValue, String tag) throws ServerFault {

	}

	@Override
	public void onServerAssigned(BmContext context, ItemValue<Server> itemValue, ItemValue<Domain> domain, String tag)
			throws ServerFault {

	}

	@Override
	public void onServerUnassigned(BmContext context, ItemValue<Server> itemValue, ItemValue<Domain> domain, String tag)
			throws ServerFault {

	}

	@Override
	public void beforeCreate(BmContext context, String uid, Server server) throws ServerFault {

	}

	@Override
	public void beforeUpdate(BmContext context, String uid, Server server, Server previous) throws ServerFault {

	}

	@Override
	public void onServerPreUnassigned(BmContext context, ItemValue<Server> server, ItemValue<Domain> domain, String tag)
			throws ServerFault {
	}

}
