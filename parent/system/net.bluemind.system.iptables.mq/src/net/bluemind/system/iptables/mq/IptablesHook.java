/*BEGIN LICENSE
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
package net.bluemind.system.iptables.mq;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.server.api.Server;
import net.bluemind.server.hook.DefaultServerHook;
import net.bluemind.system.iptables.tools.RulesUpdater;

public class IptablesHook extends DefaultServerHook {
	public IptablesHook() {
	}

	@Override
	public void onServerCreated(BmContext context, ItemValue<Server> item) throws ServerFault {
		RulesUpdater.updateIptablesScript(context, null, item.value);
	}

	@Override
	public void onServerUpdated(BmContext context, ItemValue<Server> previousValue, Server value) throws ServerFault {
		RulesUpdater.updateIptablesScript(context, previousValue.value, value);
	}

	@Override
	public void onServerDeleted(BmContext context, ItemValue<Server> server) throws ServerFault {
		RulesUpdater.updateIptablesScript(context, server.value, null);
	}
}
