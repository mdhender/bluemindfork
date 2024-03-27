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
package net.bluemind.eas.command.folder.crud;

import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import net.bluemind.eas.http.AuthorizedDeviceQuery;
import net.bluemind.eas.http.IEasRequestEndpoint;
import net.bluemind.eas.http.wbxml.WbxmlHandlerBase;
import net.bluemind.eas.protocol.ProtocolExecutor;
import net.bluemind.eas.utils.EasLogUser;

public class FolderDeleteEndpoint extends WbxmlHandlerBase implements IEasRequestEndpoint {

	private static final Logger logger = LoggerFactory.getLogger(FolderDeleteEndpoint.class);
	private FolderDeleteProtocol protocol;

	public FolderDeleteEndpoint() {
		protocol = new FolderDeleteProtocol();
	}

	@Override
	public void handle(AuthorizedDeviceQuery dq, Document doc, String userLogin) {
		if (logger.isDebugEnabled()) {
			EasLogUser.logDebugAsUser(userLogin, logger, "FolderDelete with protocol...");
		}
		ProtocolExecutor.run(dq, doc, protocol);
	}

	@Override
	public Collection<String> supportedCommands() {
		return List.of("FolderDelete");
	}

	@Override
	public boolean acceptsVersion(double protocolVersion) {
		return true;
	}

}
