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
package net.bluemind.eas.http.tests;

import java.util.HashMap;

import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.impl.Base64;

import com.google.common.collect.ImmutableMap;

import junit.framework.TestCase;
import net.bluemind.eas.http.AuthenticatedEASQuery;
import net.bluemind.eas.http.EasUrls;
import net.bluemind.eas.http.query.EASQueryBuilder;
import net.bluemind.eas.testhelper.mock.RequestObject;
import net.bluemind.eas.testhelper.mock.RequestObject.HttpMethod;
import net.bluemind.eas.testhelper.mock.RequestsFactory;
import net.bluemind.vertx.common.http.BasicAuthHandler.AuthenticatedRequest;

public class QueryBuilderTests extends TestCase {

	private RequestsFactory rf;

	public void setUp() {
		rf = new RequestsFactory("admin@vagrant.vmw", "admin", "http://bm3.vagrant.vmw");
	}

	public void testSimpleQuery() {
		String devId = "APPL" + System.currentTimeMillis();
		String devType = "iPhone " + System.currentTimeMillis();
		AuthenticatedRequest req = rf.authenticated(devId, devType,
				ImmutableMap.of("X-Ms-PolicyKey", "999", "MS-ASProtocolVersion", "14.1"),
				ImmutableMap.of("Cmd", "FolderSync"));
		AuthenticatedEASQuery decoded = EASQueryBuilder.from(req);
		assertNotNull(decoded);
		assertEquals(999, decoded.policyKey().longValue());
		assertEquals(14.1, decoded.protocolVersion());
		assertEquals("FolderSync", decoded.command());
		assertEquals(devId, decoded.deviceIdentifier());
		assertEquals(devType, decoded.deviceType());
	}

	public void testEmptyQuery() {
		HttpServerRequest req = new RequestObject(HttpMethod.OPTIONS, new HashMap<String, String>(), rf.baseUrl,
				EasUrls.ROOT, new HashMap<String, String>());
		AuthenticatedRequest ar = new AuthenticatedRequest(req, "admin@vagrant.vmw", "admin");
		AuthenticatedEASQuery decoded = EASQueryBuilder.from(ar);
		assertNotNull(decoded);
		assertEquals(0.0, decoded.protocolVersion());
		assertEquals("admin", decoded.sid());
		assertEquals("admin@vagrant.vmw", decoded.loginAtDomain());
	}

	public void testBase64Query() {
		AuthenticatedRequest req = rf.authenticated(ImmutableMap.<String, String>of(),
				"jAAJBAp2MTQwRGV2aWNlAApTbWFydFBob25l");
		AuthenticatedEASQuery decoded = EASQueryBuilder.from(req);
		assertNotNull(decoded);
		assertNull(decoded.policyKey());
		assertEquals(14.0, decoded.protocolVersion());
		assertEquals("Sync", decoded.command());
		String plainDevId = new String(Base64.decode(decoded.deviceIdentifier()));
		assertEquals("v140Device", plainDevId);
		assertEquals("SmartPhone", decoded.deviceType());
	}

}
