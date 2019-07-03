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

import org.w3c.dom.Document;

import net.bluemind.eas.http.AuthorizedDeviceQuery;
import net.bluemind.eas.protocol.ProtocolExecutor;
import net.bluemind.eas.testhelper.mock.ResponseObject;

public class BrokenProtocolTests extends AbstractProtocolTests {

	public void testBrokenParser() throws Exception {
		Document doc = load("settings/user_dev.xml");
		AuthorizedDeviceQuery query = query("Settings");
		BrokenParserProtocol broken = new BrokenParserProtocol();
		ProtocolExecutor.run(query, doc, broken);
		ResponseObject ro = (ResponseObject) query.request().response();
		ro.waitForIt(1, TimeUnit.SECONDS);
		assertEquals(500, ro.getStatusCode());
	}

	public void testBrokenExecute() throws Exception {
		Document doc = load("settings/user_dev.xml");
		AuthorizedDeviceQuery query = query("Settings");
		BrokenExecutorProtocol broken = new BrokenExecutorProtocol();
		ProtocolExecutor.run(query, doc, broken);
		ResponseObject ro = (ResponseObject) query.request().response();
		ro.waitForIt(1, TimeUnit.SECONDS);
		assertEquals(500, ro.getStatusCode());
	}

	public void testBrokenWrite() throws Exception {
		Document doc = load("settings/user_dev.xml");
		AuthorizedDeviceQuery query = query("Settings");
		BrokenWriterProtocol broken = new BrokenWriterProtocol();
		ProtocolExecutor.run(query, doc, broken);
		ResponseObject ro = (ResponseObject) query.request().response();
		ro.waitForIt(1, TimeUnit.SECONDS);
		assertEquals(500, ro.getStatusCode());
	}

}
