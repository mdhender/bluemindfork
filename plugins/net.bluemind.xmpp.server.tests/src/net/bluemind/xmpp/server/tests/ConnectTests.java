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

import java.util.Collection;

import org.jivesoftware.smack.AccountManager;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterGroup;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.Form;
import org.jivesoftware.smackx.muc.MultiUserChat;

import net.bluemind.utils.Trust;

public class ConnectTests extends ServerTest {

	public void testConnectAdminNoTLS() throws XMPPException {
		ConnectionConfiguration cc = new ConnectionConfiguration(p("xmpp.server"), 5222, p("domain"));
		cc.setSecurityMode(SecurityMode.disabled);
		cc.setSendPresence(false);
		cc.setRosterLoadedAtLogin(false);
		XMPPConnection con = new XMPPConnection(cc);
		con.connect();
		con.login("admin@" + p("domain"), "admin");
		con.disconnect();
	}

	public void testConnectAdminWithTLS() throws XMPPException {
		ConnectionConfiguration cc = new ConnectionConfiguration(p("xmpp.server"), 5222, p("domain"));
		cc.setSecurityMode(SecurityMode.enabled);
		cc.setSendPresence(false);
		cc.setRosterLoadedAtLogin(false);
		cc.setCustomSSLContext(Trust.createSSLContext());
		XMPPConnection con = new XMPPConnection(cc);

		try {
			con.connect();
		} catch (XMPPException xe) {
			printXMPPException(xe);
			fail(xe.getMessage());
		}

		con.login("admin@" + p("domain"), "admin");
		con.disconnect();
	}

	public void testConnectTwo() throws XMPPException {
		XMPPConnection con1 = xc(p("jid1") + "@" + p("domain"), p("pass1"));
		assertNotNull(con1);
		XMPPConnection con2 = xc(p("jid2") + "@" + p("domain"), p("pass2"));
		assertNotNull(con2);

		AccountManager acm = con1.getAccountManager();
		assertNotNull(acm);
		Roster r1 = con1.getRoster();
		Collection<RosterEntry> roe = r1.getEntries();
		System.out.println("roster entries: " + roe.size());
		Collection<RosterGroup> rog = r1.getGroups();
		System.out.println("roster groups: " + rog.size());
		r1.createEntry(p("jid2") + "@" + p("domain"), "Psi " + System.currentTimeMillis(), new String[] { "Dudes" });
		assertTrue(r1.contains(p("jid2") + "@" + p("domain")));
		ChatManager cm = con1.getChatManager();
		assertNotNull(cm);

		con1.disconnect();
		con2.disconnect();
	}

	public void testInstantRoom() throws XMPPException {
		XMPPConnection con = xc(p("jid1") + "@" + p("domain"), p("pass1"));
		MultiUserChat muc = new MultiUserChat(con, "room" + System.currentTimeMillis() + "@muc." + p("domain"));
		muc.create(p("jid1"));
		muc.sendConfigurationForm(new Form(Form.TYPE_SUBMIT));

		con.disconnect();
	}

}
