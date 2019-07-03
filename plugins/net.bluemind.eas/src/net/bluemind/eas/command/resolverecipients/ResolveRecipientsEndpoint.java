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
package net.bluemind.eas.command.resolverecipients;

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
 * Used, at least for free busy requests:
 * 
 * <pre>
 * <?xml version="1.0" encoding="UTF-8"?>
 * <ResolveRecipients>
 *   <To>u2@vagrant.vmw</To>
 *   <Options>
 *     <MaxAmbiguousRecipients>0</MaxAmbiguousRecipients>
 *     <Availability>
 *     <StartTime>2015-08-30T13:00:00.000Z</StartTime>
 *     <EndTime>2015-08-30T14:30:00.000Z</EndTime>
 *     </Availability>
 *   </Options>
 * </ResolveRecipients>
 * </pre>
 *
 */
public class ResolveRecipientsEndpoint extends WbxmlHandlerBase implements IEasRequestEndpoint {

	private static final Logger logger = LoggerFactory.getLogger(ResolveRecipientsEndpoint.class);
	private ResolveRecipientsProtocol protocol;

	public ResolveRecipientsEndpoint() {
		protocol = new ResolveRecipientsProtocol();
	}

	@Override
	public void handle(AuthorizedDeviceQuery dq, Document doc) {
		if (logger.isDebugEnabled()) {
			logger.debug("ResolveRecipients with protocol...");
		}
		ProtocolExecutor.run(dq, doc, protocol);
	}

	@Override
	public Collection<String> supportedCommands() {
		return ImmutableList.of("ResolveRecipients");
	}

	@Override
	public boolean acceptsVersion(double protocolVersion) {
		return protocolVersion >= 14;
	}

}
