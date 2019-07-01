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
package net.bluemind.backend.cyrus.internal.files;

import net.bluemind.config.Token;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.server.api.IServer;

/**
 * /etc/cyrus-proxypassword handler
 * 
 * 
 */
public class CyrusProxyPassword extends AbstractConfFile {
	private static final String FILE_PATH = "/etc/cyrus-proxypassword";

	public CyrusProxyPassword(IServer service, String serverUid) throws ServerFault {
		super(service, serverUid);
	}

	@Override
	public void write() throws ServerFault {

		String cyrusReplication = "proxy_password: " + Token.admin0();

		service.writeFile(serverUid, FILE_PATH, cyrusReplication.getBytes());
		service.submit(serverUid, "chown cyrus:mail " + FILE_PATH);
		service.submit(serverUid, "chmod 640 " + FILE_PATH);
	}

}
