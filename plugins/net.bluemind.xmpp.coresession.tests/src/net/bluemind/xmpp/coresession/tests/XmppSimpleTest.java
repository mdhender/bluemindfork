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
package net.bluemind.xmpp.coresession.tests;

import java.security.SecureRandom;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster.SubscriptionMode;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;

public class XmppSimpleTest extends BaseXmppTests {

	private static final String HOST = "vm1";
	private static final int PORT = 5222;
	private static final String XMPP_NAME = "bm.lan";

	public class RequestListener implements PacketListener {
		@Override
		public void processPacket(Packet pack) {
			Presence pres = (Presence) pack;
			if (pres.getType() != null && pres.getType().equals(Presence.Type.subscribe)) {
				// user with jid pres.getFrom() rejected your request.
				System.out.println(pres.getFrom() + " ask sub");
			}
		}
	}

	// @Test
	public void testSub() throws Exception {
		Thread.sleep(1000);
		System.out.println("user1 " + user1.login + "@" + domainName);
		XMPPTCPConnection conn1 = login(user1.login, "password");
		XMPPTCPConnection conn2 = login(user2.login, "password");

		// somewhere in code to register the request listener after login
		conn2.addPacketListener(new RequestListener(), new PacketTypeFilter(Presence.class));

		conn1.getRoster().setSubscriptionMode(SubscriptionMode.manual);
		conn1.getRoster().createEntry(user2.login + "@" + domainName, user2.login, new String[] {});

		Thread.sleep(2000000);
	}

	private XMPPTCPConnection login(String login, String password) throws Exception {
		ConnectionConfiguration config = new ConnectionConfiguration(HOST, PORT, XMPP_NAME);

		// trust all
		SSLContext sc = SSLContext.getInstance("TLS");
		HostnameVerifier ver = new HostnameVerifier() {

			public boolean verify(String hostname, SSLSession session) {
				return true;
			}

		};
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
			}

			public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
			}
		} };

		config.setHostnameVerifier(ver);
		sc.init(null, trustAllCerts, new SecureRandom());

		config.setCustomSSLContext(sc);
		config.setDebuggerEnabled(true);
		XMPPTCPConnection xmppConn = new XMPPTCPConnection(config);
		xmppConn.connect();

		xmppConn.login(login, password, "Bluemind");
		return xmppConn;
	}

	// @Test
	public void testDesTrucs() throws Exception {
		ConnectionConfiguration config = new ConnectionConfiguration(HOST, PORT, XMPP_NAME);

		// trust all
		SSLContext sc = SSLContext.getInstance("TLS");
		HostnameVerifier ver = new HostnameVerifier() {

			public boolean verify(String hostname, SSLSession session) {
				return true;
			}

		};
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
			}

			public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
			}
		} };

		config.setHostnameVerifier(ver);
		sc.init(null, trustAllCerts, new SecureRandom());

		config.setCustomSSLContext(sc);
		config.setDebuggerEnabled(true);
		XMPPTCPConnection xmppConn = new XMPPTCPConnection(config);
		xmppConn.connect();

		xmppConn.login("admin", "admin", "Bluemind");

		System.out.println(xmppConn.getRoster().getEntryCount());
		RosterEntry entry = xmppConn.getRoster().getEntries().iterator().next();
		System.out.println(entry);
	}
}
