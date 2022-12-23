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
package net.bluemind.imap;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import org.junit.Assert;

import net.bluemind.imap.sieve.SieveClient;
import net.bluemind.imap.sieve.SieveClient.SieveConnectionData;
import net.bluemind.imap.sieve.SieveScript;

public class SieveClientTests extends SieveTestCase {

	private SieveClient sc;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		SieveConnectionData connectionData = new SieveConnectionData(testLogin, testPass, cyrusIp);
		sc = new SieveClient(connectionData);
		Assert.assertTrue(sc.login());
	}

	public void testEmpty() {
		// empty test to validate setup & teardown
	}

	public void testPutscript() {
		String name = "test." + System.currentTimeMillis() + ".sieve";
		final String content = "" // test script
				+ "require [ \"fileinto\", \"imapflags\", "
				// +"\"body\", " // cyrus 2.3 extensions ?!
				+ "\"vacation\" ];\n" // extensions
		// +"if body :text :contains \"viagra\"{\n discard;\n}\n"
				+ "if size :over 500K {\n   setflag \"\\\\Flagged\";\n}\n" + "fileinto \"INBOX\";\n";
		InputStream contentStream = new ByteArrayInputStream(content.getBytes());
		boolean ret = sc.putscript(name, contentStream);
		assertTrue(ret);
		assertTrue(sc.listscripts().size() > 0);

		boolean present = false;
		for (SieveScript ss : sc.listscripts()) {
			if (ss.getName().equals(name)) {
				present = true;
				String v = sc.getScript(ss.getName());
				// replace \r because someone introduce them in the response.
				// Don't know why
				assertEquals(content, v.replace("\r", ""));
				break;
			}
		}
		Assert.assertTrue(present);
	}

	public void testPutscript_invalid() {
		String name = "test." + System.currentTimeMillis() + ".sieve";
		String content = "" // test script
				+ "#comment \n" + "InvalidSieveCommand\n";
		InputStream contentStream = new ByteArrayInputStream(content.getBytes());
		System.out.println("before put script");
		boolean ret = sc.putscript(name, contentStream);
		assertFalse(ret);
	}

	public void testListscripts() {
		List<SieveScript> list = sc.listscripts();

		for (SieveScript script : list) {
			System.err.println(script.getName() + " :" + script.isActive());
			System.out.println(sc.getScript(script.getName()));
		}
	}

	public void testListscriptsBenchmark() {
		int COUNT = 1000;

		sc.logout();
		long time = System.currentTimeMillis();

		Assert.assertTrue(sc.login());
		int old = sc.listscripts().size();
		sc.logout();

		for (int i = 0; i < COUNT; i++) {
			boolean loginOk = sc.login();
			assertTrue(loginOk);
			int cur = sc.listscripts().size();
			sc.logout();
			assertEquals(old, cur);
			old = cur;
		}

		time = System.currentTimeMillis() - time;
		System.out.println(
				COUNT + " listscripts done in " + time + "ms. Performing at " + ((1000.0 * COUNT) / time) + "/s.");
	}

	public void testListAndDeleteAll() {
		List<SieveScript> scripts = sc.listscripts();
		System.err.println("needs to delete " + scripts.size() + " scripts.");
		// on désactive les scripts
		sc.activate("");

		for (SieveScript ss : scripts) {
			Assert.assertTrue(sc.deletescript(ss.getName()));
		}
		scripts = sc.listscripts();
		assertTrue(scripts.size() == 0);
	}

	@Override
	protected void tearDown() throws Exception {
		sc.logout();
		sc = null;
		super.tearDown();
	}

}
