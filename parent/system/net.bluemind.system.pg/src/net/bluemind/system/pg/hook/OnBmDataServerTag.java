/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.system.pg.hook;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.server.api.Server;
import net.bluemind.server.hook.DefaultServerHook;
import net.bluemind.system.pg.PostgreSQLService;

public class OnBmDataServerTag extends DefaultServerHook {

	@Override
	public void onServerTagged(BmContext context, ItemValue<Server> itemValue, String tag) throws ServerFault {
		if (!"bm/pgsql-data".equals(tag)) {
			return;
		}

		PostgreSQLService service = new PostgreSQLService();
		service.addDataServer(itemValue, "bj-data");

	}

}
