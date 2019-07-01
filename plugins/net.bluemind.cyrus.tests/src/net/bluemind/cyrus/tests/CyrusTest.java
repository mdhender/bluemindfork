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
package net.bluemind.cyrus.tests;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import junit.framework.TestCase;
import net.bluemind.core.api.AccessToken;
import net.bluemind.core.api.ICore;
import net.bluemind.core.api.fault.AuthFault;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.api.system.Host;
import net.bluemind.core.api.user.MailRouting;
import net.bluemind.core.api.user.User;
import net.bluemind.core.client.CoreClient;
import net.bluemind.core.client.locators.SystemLocator;
import net.bluemind.core.client.system.SystemClient;
import net.bluemind.imap.Flag;
import net.bluemind.imap.FlagsList;
import net.bluemind.imap.IMAPException;
import net.bluemind.imap.StoreClient;
import net.bluemind.locator.client.LocatorClient;

import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.dom.Header;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.MessageBuilder;
import org.apache.james.mime4j.dom.MessageServiceFactory;
import org.apache.james.mime4j.dom.MessageWriter;
import org.apache.james.mime4j.dom.TextBody;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.field.Fields;
import org.apache.james.mime4j.message.BasicBodyFactory;
import org.apache.james.mime4j.util.MimeUtil;

public abstract class CyrusTest extends TestCase {

	protected String testLogin;
	protected String testPass;
	protected AccessToken gtok;
	protected AccessToken token;
	protected User loggedIn;
	protected int iterations;
	protected StoreClient imap;
	protected ICore core;
	protected String imapHost;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		Properties props = loadTestProps("tests.properties");
		if (props.isEmpty()) {
			System.out
					.println("tests.properties not found, using tests.properties.sample");
			props = loadTestProps("tests.properties.sample");
		}
		this.testLogin = props.get("login").toString();
		this.testPass = props.get("pass").toString();
		System.out.println("Test " + getName() + " credentials: " + testLogin
				+ " / " + testPass);

		String coreUrl = props.getProperty("coreUrl");
		this.iterations = Integer.parseInt(props.getProperty("iterations"));

		SystemClient system = new SystemLocator().locate(coreUrl);
		this.gtok = system.login("admin0@global.virt", "admin", "ju-"
				+ getName());

		this.token = system.login(testLogin, testPass, "ju-" + getName());
		this.core = CoreClient.newCore(coreUrl);

		this.loggedIn = core.getUser().getUserFromId(token, token.getUserId());

		// get rid of the mail Q
		System.out.println("Clearing mailqueue...");
		List<Host> allPostfix = system.findAssignedHosts(gtok, "mail/smtp",
				loggedIn.getDomain().getName());
		for (Host h : allPostfix) {
			system.nodeExecuteCommand(gtok, h.getId(), "postsuper -d ALL");
		}
		System.out.println("Mailqueue cleared.");

		LocatorClient lc = new LocatorClient();
		this.imapHost = lc.locateHost("mail/imap", testLogin);
		System.out.println("IMAP: " + imapHost);
		this.imap = new StoreClient(imapHost, 143, testLogin, testPass);
		boolean loggedInImap = imap.login(false);
		assertTrue(loggedInImap);

	}

	protected long addOneEmail(String folder) throws MimeException, IOException {
		WriteOp wo = new WriteOp() {
			@Override
			public void doWrite(OutputStream out) throws Exception {
				MessageServiceFactory msf = MessageServiceFactory.newInstance();
				Message mm = getRandomMessage(msf);
				MessageWriter writer = msf.newMessageWriter();
				writer.writeMessage(mm, out);
			}
		};
		InputStream in = wo.write();
		return imap.append(folder, in, new FlagsList());
	}

	protected void deleteMail(int uid) throws IMAPException {
		imap.select("INBOX");
		FlagsList fl = new FlagsList();
		fl.add(Flag.DELETED);
		imap.uidStore(Arrays.asList(uid), fl, true);
		imap.expunge();
	}

	protected User newMailUser() throws AuthFault, ServerFault {
		User u = new User();
		long t = System.currentTimeMillis();
		String l = "a.b" + t;
		u.setLogin(l);
		u.setLastname(l);
		u.setDomain(loggedIn.getDomain());
		u.setPerms("user");
		u.setPassword(l);
		u.setMailRouting(MailRouting.INTERNAL);
		u = core.getUser().create(token, u);
		assertTrue(u.getId() > 0);
		System.out.println("Created " + u.getReservedBoxName());
		System.out.println("Connecting to imap host: " + imapHost);
		return u;
	}

	private Message getRandomMessage(MessageServiceFactory msf)
			throws UnsupportedEncodingException {
		MessageBuilder builder = msf.newMessageBuilder();
		Message mm = builder.newMessage();
		BasicBodyFactory bbf = new BasicBodyFactory();
		Header h = builder.newHeader();
		// we want the mail to have an encoding that cannot be included directly
		// in utf-8 xml
		h.setField(Fields.contentType("text/html; charset=ISO-8859-15"));
		h.setField(Fields.contentTransferEncoding(MimeUtil.ENC_8BIT));
		mm.setHeader(h);
		TextBody text = bbf
				.textBody(
						"<html><body>€uro pouic accentu&eacute; with entity</body></html>",
						"ISO-8859-15");
		mm.setBody(text);
		Date now = new Date();
		mm.setSubject("Pouic €uro (" + now + ")");
		mm.setDate(now);
		Mailbox mbox = new Mailbox("Rob Malone (accentué)", "bm.junit.roberto",
				"gmail.com");
		mm.setFrom(mbox);

		return mm;
	}

	@Override
	protected void tearDown() throws Exception {
		if (imap != null) {
			imap.logout();
		}

		// purge temporary crap
		File[] toDel = new File("/tmp").listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.startsWith("pushresp") && name.endsWith(".bin");
			}
		});
		for (File f : toDel) {
			if (f.exists() && f.isFile()) {
				f.delete();
			}
		}

		super.tearDown();
	}

	private Properties loadTestProps(String propFileName) throws IOException {
		Properties props = new Properties();
		InputStream in = CyrusTest.class.getClassLoader().getResourceAsStream(
				"data/" + propFileName);
		if (in != null) {
			props.load(in);
			in.close();
		}
		return props;
	}

}
