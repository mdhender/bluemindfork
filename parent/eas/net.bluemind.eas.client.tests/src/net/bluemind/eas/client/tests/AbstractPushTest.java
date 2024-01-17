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
package net.bluemind.eas.client.tests;

import java.io.InputStream;
import java.util.Properties;

import org.w3c.dom.Document;

import junit.framework.TestCase;
import net.bluemind.eas.client.OPClient;
import net.bluemind.eas.client.ProtocolVersion;

public class AbstractPushTest extends TestCase {

	protected OPClient opc;

	protected AbstractPushTest() {
	}

	// "POST
	// /Microsoft-Server-ActiveSync?User=thomas@zz.com&DeviceId=Appl87837L1XY7H&DeviceType=iPhone&Cmd=Sync
	// HTTP/1.1"

	private String p(Properties p, String k) {
		return p.getProperty(k);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		InputStream in = loadDataFile("test.properties");
		Properties p = new Properties();
		p.load(in);
		in.close();

		String login = p(p, "login");
		String password = p(p, "password");
		String devId = p(p, "devId");
		String devType = p(p, "devType");
		String userAgent = p(p, "userAgent");
		String url = p(p, "url");

		opc = new OPClient(login, password, devId, devType, userAgent, url);

		System.err.println("l: " + login + " p: " + password + " di: " + devId + " dt: " + devType + " ua: " + userAgent
				+ " url: " + url);
	}

	@Override
	protected void tearDown() throws Exception {
		opc.destroy();
		opc = null;
		super.tearDown();
	}

	protected InputStream loadDataFile(String name) {
		return AbstractPushTest.class.getClassLoader().getResourceAsStream("data/" + name);
	}

	public void optionsQuery() throws Exception {
		opc.options();
	}

	public Document postXml(String namespace, Document doc, String cmd, String policyKey, String pv, boolean multipart)
			throws Exception {
		if ("2.5".equals(pv)) {
			opc.setProtocolVersion(ProtocolVersion.V25);
		} else if ("12.0".equals(pv)) {
			opc.setProtocolVersion(ProtocolVersion.V120);
		} else {
			opc.setProtocolVersion(ProtocolVersion.V121);
		}
		return opc.postXml(namespace, doc, cmd, policyKey, multipart);
	}

	public Document postXml(String namespace, Document doc, String cmd) throws Exception {
		opc.setProtocolVersion(ProtocolVersion.V121);
		return opc.postXml(namespace, doc, cmd);
	}

	public Document postMultipartXml(String namespace, Document doc, String cmd) throws Exception {
		opc.setProtocolVersion(ProtocolVersion.V121);
		return opc.postXml(namespace, doc, cmd, null, true);
	}

	public Document postXml25(String namespace, Document doc, String cmd) throws Exception {
		opc.setProtocolVersion(ProtocolVersion.V25);
		return opc.postXml(namespace, doc, cmd);
	}

	public Document postXml120(String namespace, Document doc, String cmd) throws Exception {
		opc.setProtocolVersion(ProtocolVersion.V120);
		return opc.postXml(namespace, doc, cmd);
	}

	public byte[] postGetAttachment(String attachmentName) throws Exception {
		return opc.postGetAttachment(attachmentName);
	}

}
