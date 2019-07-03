/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2016
  *
  * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License)
  * or the CeCILL as published by CeCILL.info (version 2 of the License).
  *
  * There are special exceptions to the terms and conditions of the
  * licenses as they are applied to this program. See LICENSE.txt in
  * the directory of this program distribution.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.ui.adminconsole.monitoring.models;

import java.util.HashMap;
import java.util.Map;

import net.bluemind.monitoring.api.ServerInformation;

/**
 * A server information message entry represents a single line in the message
 * list box widget
 * 
 * @author vincent
 *
 */
public class ServerInformationMessageEntry implements Comparable<ServerInformationMessageEntry> {

	public ServerInformation serverInfo;
	public int idMessage;
	public Map<String, String> param;

	public ServerInformationMessageEntry(ServerInformation srvInfo, int idMessage) {
		this.serverInfo = srvInfo;
		this.idMessage = idMessage;
		this.param = new HashMap<String, String>();
		this.param.put("plugin", this.serverInfo.plugin);
		this.param.put("service", this.serverInfo.service);

	}

	@Override
	public int compareTo(ServerInformationMessageEntry entry) {
		if (this.serverInfo.status.getValue() < entry.serverInfo.status.getValue()) {
			return -1;
		} else if (this.serverInfo.status.getValue() == entry.serverInfo.status.getValue()) {
			return 0;
		} else {
			return 1;
		}

	}

}
