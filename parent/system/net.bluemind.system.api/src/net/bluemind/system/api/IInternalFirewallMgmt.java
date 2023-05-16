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
package net.bluemind.system.api;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.task.api.TaskRef;

@BMApi(version = "3", internal = true)
@Path("/system/firewall")
public interface IInternalFirewallMgmt {
	/**
	 * Update firewall rules
	 * 
	 * @return
	 * @throws ServerFault
	 */
	@POST
	@Path("_updatefirewallrules")
	public TaskRef updateFirewallRules() throws ServerFault;

}
