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
package net.bluemind.eas.protocol.tests;

import java.util.concurrent.TimeUnit;

import org.vertx.java.core.buffer.Buffer;
import org.w3c.dom.Document;

import net.bluemind.eas.command.settings.SettingsProtocol;
import net.bluemind.eas.http.AuthorizedDeviceQuery;
import net.bluemind.eas.protocol.ProtocolExecutor;
import net.bluemind.eas.testhelper.mock.ResponseObject;
import net.bluemind.eas.wbxml.WBXMLTools;

public class SettingsProtocolTests extends AbstractProtocolTests {

	public void testUserAndDev() throws Exception {
		Document doc = load("settings/user_dev.xml");
		AuthorizedDeviceQuery query = query("Settings");
		SettingsProtocol sp = new SettingsProtocol();
		ProtocolExecutor.run(query, doc, sp);
		ResponseObject ro = (ResponseObject) query.request().response();
		Buffer content = ro.waitForIt(1, TimeUnit.SECONDS);
		assertNotNull(content);
		Document responseXml = WBXMLTools.toXml(content.getBytes());
		assertNotNull(responseXml);
	}

}
