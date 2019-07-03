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
package net.bluemind.eas.command.mail.smartreply;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.google.common.collect.ImmutableList;

import net.bluemind.eas.http.AuthorizedDeviceQuery;
import net.bluemind.eas.http.IEasRequestEndpoint;
import net.bluemind.eas.http.wbxml.WbxmlHandlerBase;
import net.bluemind.eas.protocol.ProtocolExecutor;

/**
 * 
 * 
 * <code>
 * <?xml version="1.0" encoding="UTF-8"?>
 * <SmartReply>
 *   <ClientId>SendMail-3443097409678</ClientId>
 *   <SaveInSentItems/>
 *   <Source>
 *     <ItemId>11:1</ItemId>
 *     <FolderId>11</FolderId>
 *   </Source>
 *   <Mime>RGF0ZT ... 2Q2dvPQ0K</Mime>
 * </SmartReply>
 * </code>
 * 
 */
public class SmartReplyEndpoint extends WbxmlHandlerBase implements IEasRequestEndpoint {

	private static final Logger logger = LoggerFactory.getLogger(SmartReplyEndpoint.class);
	private SmartReplyProtocol protocol;

	public SmartReplyEndpoint() {
		protocol = new SmartReplyProtocol();
	}

	@Override
	public void handle(AuthorizedDeviceQuery dq, Document doc) {
		if (logger.isDebugEnabled()) {
			logger.debug("SmartReply with protocol...");
		}
		ProtocolExecutor.run(dq, doc, protocol);
	}

	@Override
	public Collection<String> supportedCommands() {
		return ImmutableList.of("SmartReply");
	}

	@Override
	public boolean acceptsVersion(double protocolVersion) {
		return protocolVersion >= 14;
	}

}
