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
package net.bluemind.eas.command.provision;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import net.bluemind.eas.dto.NamespaceMapping;
import net.bluemind.eas.impl.Responder;
import net.bluemind.eas.utils.DOMUtils;

public class ProvisionHelper {

	private static final Logger logger = LoggerFactory.getLogger(ProvisionHelper.class);

	public static void forceProvisionProto14(String cmd, Responder resp) {
		logger.info("Force provision protocol 14.1");
		errorStatus(cmd, resp, 142);
	}

	public static void forceWipeProto14(String cmd, Responder resp) {
		logger.info("Force wipe protocol 14.1");
		errorStatus(cmd, resp, 140);
	}

	private static void errorStatus(String cmd, Responder resp, int status) {
		NamespaceMapping mapping = NamespaceMapping.valueOf(cmd);
		Document doc = DOMUtils.createDoc(mapping.namespace(), mapping.root());
		DOMUtils.createElementAndText(doc.getDocumentElement(), "Status", "" + status);
		resp.sendResponse(mapping, doc);
	}

}
