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
package net.bluemind.xmpp.server.tests;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.SmackError;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.packet.StreamError;
import org.jivesoftware.smack.packet.XMPPError;

import junit.framework.TestCase;
import net.bluemind.xmpp.server.Activator;

public abstract class ServerTest extends TestCase {

	private Properties props;
	private static boolean running;

	public void setUp() {
		props = new Properties();
		InputStream in = ServerTest.class.getClassLoader().getResourceAsStream("data/tests.props");
		if (in == null) {
			in = ServerTest.class.getClassLoader().getResourceAsStream("data/tests.props.sample");
			System.out.println("using test.props.sample");
		} else {
			System.out.println("using test.props");
		}
		try {
			props.load(in);
		} catch (IOException e1) {
			e1.printStackTrace();
			fail();
		}

		if ("true".equals(p("start.server")) && !running) {
			Activator.loadTrick();
			sleep(4000);
			running = true;
		} else {
			System.out.println("Not starting server.");
		}

		System.out.println("=====SETUP=====");
	}

	protected void printXMPPException(XMPPException xe) {
		xe.printStackTrace();
		SmackError sme = xe.getSmackError();
		StreamError ste = xe.getStreamError();
		XMPPError xme = xe.getXMPPError();
		System.out.println("smack: " + sme + ", stream: " + ste + ", xmpp: " + xme);
		if (xme != null) {
			System.out.println(
					"xme.cond: " + xme.getCondition() + ", msg: " + xme.getMessage() + ", type: " + xme.getType());
			List<PacketExtension> exts = xme.getExtensions();
			if (exts != null) {
				System.out.println("Got some extensions: " + exts.size());
				for (PacketExtension pe : exts) {
					System.out.println("pe: " + pe);
				}
			} else {
				System.out.println("No extended errors");
			}
		}
	}

	protected void sleep(long ms) {
		try {
			System.out.println("Sleeping " + ms + "ms.");
			Thread.sleep(ms);
		} catch (InterruptedException e) {
		}
	}

	protected XMPPConnection xc(String jid, String pass) throws XMPPException {
		ConnectionConfiguration cc = new ConnectionConfiguration(p("xmpp.server"), 5222, p("domain"));
		cc.setSecurityMode(SecurityMode.disabled);
		cc.setSendPresence(false);
		cc.setRosterLoadedAtLogin(false);
		// cc.setCustomSSLContext(Trust.createSSLContext());
		XMPPConnection con = new XMPPConnection(cc);
		try {
			con.connect();
		} catch (XMPPException xe) {
			printXMPPException(xe);
			fail(xe.getMessage());
		}

		con.login(jid, pass);
		return con;
	}

	protected String p(String k) {
		return props.getProperty(k);
	}

	public void tearDown() {
		System.out.println("=====TEARDOWN=====");
	}

}
