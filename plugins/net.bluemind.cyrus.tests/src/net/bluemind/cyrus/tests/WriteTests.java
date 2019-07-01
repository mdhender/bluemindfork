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

import java.io.IOException;

import net.bluemind.core.api.fault.AuthFault;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.api.user.User;
import net.bluemind.imap.IMAPException;
import net.bluemind.imap.StoreClient;

import org.apache.james.mime4j.MimeException;

public class WriteTests extends CyrusTest {

	public void testBug4632() throws AuthFault, ServerFault, IMAPException,
			MimeException, IOException {
		System.out.println("Creating user...");
		User u = newMailUser();
		System.out.println("User created.");

		StoreClient sc = new StoreClient(imapHost, 143, u.getReservedBoxName(),
				u.getLogin());
		boolean login = sc.login();
		assertTrue(login);
		System.out.println("IMAP login is OK !");
		boolean failure = false;
		for (int i = 0; i < iterations; i++) {
			long uid = addOneEmail("Sent");
			System.out.println("Added " + uid + " to Sent folder");
			if (uid == -1) {
				failure = true;
			}
		}
		sc.logout();

		core.getUser().deleteUser(token, u.getId());
		System.out.println("Deleted " + u.getReservedBoxName());
		assertFalse("Failed to write to Sent folder", failure);
	}

}
