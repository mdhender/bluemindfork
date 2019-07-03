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

import java.util.Map;

public class AclTestCase extends CyradmTestCase {

	public void testCrudAcl() {
		try {
			boolean success = sc.setAcl(mboxCyrusName, testLogin, Acl.ALL);
			if (!success) {
				fail("error setting acl");
			}
			Map<String, Acl> acls = sc.listAcl(mboxCyrusName);
			System.err.println("acls size: " + acls.size());
			for (String consumer : acls.keySet()) {
				System.err.println("consumer: " + consumer + " => " + acls.get(consumer));
			}
			assertTrue(acls.size() > 0);
			assertTrue(acls.containsKey(testLogin));

			int size = acls.size();
			boolean rm = sc.deleteAcl(mboxCyrusName, testLogin);
			if (!rm) {
				fail("could not delete acl");
			}
			acls = sc.listAcl(mboxCyrusName);
			System.err.println("acls.size: " + acls.size());
			assertEquals(size - 1, acls.size());
		} catch (IMAPException e) {
			e.printStackTrace();
			fail("stacktrace in getAcl");
		}
	}

	public void testAclCompare() {
		Acl all = Acl.ALL;
		Acl newAcl = new Acl(all.toString());
		assertEquals(all, newAcl);
	}
}
