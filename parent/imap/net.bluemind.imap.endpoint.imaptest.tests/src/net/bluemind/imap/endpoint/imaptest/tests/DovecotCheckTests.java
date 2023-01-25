/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.imap.endpoint.imaptest.tests;

import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.imap.IMAPException;
import net.bluemind.imap.StoreClient;
import net.bluemind.imap.TaggedResult;

public class DovecotCheckTests {

	private DovecotContainer dc;

	@Before
	public void before() {
		this.dc = new DovecotContainer();
		dc.start();
	}

	@After
	public void after() {
		dc.stop();
	}

	@Test
	public void checkConnection() throws IMAPException {
		try (StoreClient sc = new StoreClient(dc.inspectAddress(), 143, "john", "pass")) {
			assertTrue(sc.login(true));
			assertTrue(sc.select("INBOX"));
			sc.create("Trash");
			selectAndReportForTrash("TRASH1", sc);
			sc.create("RootFolder");
			sc.rename("RootFolder", "Trash/RootFolder");
			selectAndReportForTrash("TRASH2", sc);

			sc.rename("Trash/RootFolder", "RootFolder");
			selectAndReportForTrash("TRASH3", sc);
		}
	}

	private void selectAndReportForTrash(String prefix, StoreClient sc) throws IMAPException {
		TaggedResult trash = sc.tagged("select Trash");
		for (String s : trash.getOutput()) {
			System.err.println(prefix + ": " + s);
		}
		assertTrue(trash.isOk());
		sc.select("INBOX");
		String listCmd = """
				LIST "" "Trash/%\"""";
		TaggedResult content = sc.tagged(listCmd);
		for (String s : content.getOutput()) {
			System.err.println("L " + prefix + ": " + s);
		}
		System.err.println(" --- ");
	}

}
