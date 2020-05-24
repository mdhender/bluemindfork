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
package net.bluemind.eas.config;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.node.api.INodeClient;
import net.bluemind.system.nginx.INginxConfigUpdater;

public class EasConfigUpdater implements INginxConfigUpdater {
	@Override
	public void updateMessageSize(INodeClient nc, long size) throws ServerFault {
		NginxEasConf conf = new NginxEasConf(nc);
		conf.setMessageSizeLimit(size);
		conf.write();
	}

	@Override
	public void updateFilehostingSize(INodeClient nc, long size) throws ServerFault {
		// Nothing to do
	}
}
