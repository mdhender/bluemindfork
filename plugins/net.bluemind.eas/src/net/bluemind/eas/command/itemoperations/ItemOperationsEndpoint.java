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
package net.bluemind.eas.command.itemoperations;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.google.common.collect.ImmutableList;

import net.bluemind.eas.http.AuthorizedDeviceQuery;
import net.bluemind.eas.http.IEasRequestEndpoint;
import net.bluemind.eas.http.wbxml.WbxmlHandlerBase;
import net.bluemind.eas.protocol.ProtocolExecutor;

public class ItemOperationsEndpoint extends WbxmlHandlerBase implements IEasRequestEndpoint {

	private static final Logger logger = LoggerFactory.getLogger(ItemOperationsEndpoint.class);
	private ItemOperationsProtocol protocol;

	public ItemOperationsEndpoint() {
		protocol = new ItemOperationsProtocol();
	}

	@Override
	public void handle(AuthorizedDeviceQuery dq, Document doc) {
		if (logger.isDebugEnabled()) {
			logger.debug("ItemOperations with protocol...");
		}
		ProtocolExecutor.run(dq, doc, protocol);
	}

	@Override
	public Collection<String> supportedCommands() {
		return ImmutableList.of("ItemOperations");
	}

	@Override
	public boolean acceptsVersion(double protocolVersion) {
		return true;
	}
}
