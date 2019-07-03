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
package net.bluemind.backend.cyrus;

import java.util.List;

import net.bluemind.backend.cyrus.internal.files.AbstractConfFile;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.server.api.IServer;

public class CyrusAdmins extends AbstractConfFile {

	public CyrusAdmins(IServer service, String serverUid) throws ServerFault {
		super(service, serverUid);
	}

	@Override
	public void write() throws ServerFault {

		List<ItemValue<Domain>> domains = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomains.class).all();

		StringBuilder admins = new StringBuilder();
		admins.append("admins: ");
		admins.append("admin0"); // default admin

		for (ItemValue<Domain> domain : domains) {
			if (domain.value.global) {
				continue;
			}

			admins.append(" ");
			admins.append("bmhiddensysadmin@").append(domain.value.name);
		}

		admins.append("\n");
		service.writeFile(serverUid, "/etc/cyrus-admins", admins.toString().getBytes());
	}
}
