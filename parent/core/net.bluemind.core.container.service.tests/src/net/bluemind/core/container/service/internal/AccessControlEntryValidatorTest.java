/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.core.container.service.internal;

import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;

public class AccessControlEntryValidatorTest {

	private AccessControlEntryValidator validator;

	@Before
	public void before() throws Exception {
		validator = new AccessControlEntryValidator("bm.lan");
	}

	@Test
	public void validate_MailboxAcl_NoPublicSharing_Ok() {
		Container c = Container.create("uid", "mailboxacl", "name", "owner");

		ArrayList<AccessControlEntry> accessControlEntries = new ArrayList<AccessControlEntry>();
		accessControlEntries.add(AccessControlEntry.create(UUID.randomUUID().toString(), Verb.Write));
		accessControlEntries.add(AccessControlEntry.create(UUID.randomUUID().toString(), Verb.Read));
		try {
			validator.validate(c, accessControlEntries);
		} catch (ServerFault sf) {
			fail();
		}
	}

	@Test
	public void validate_MailboxAcl_NoPublicSharing_Forbidden() throws SQLException {
		Container c = Container.create("uid", "mailboxacl", "name", "owner");

		ArrayList<AccessControlEntry> accessControlEntries = new ArrayList<AccessControlEntry>();
		accessControlEntries.add(AccessControlEntry.create("bm.lan", Verb.Write));
		accessControlEntries.add(AccessControlEntry.create(UUID.randomUUID().toString(), Verb.Read));
		try {
			validator.validate(c, accessControlEntries);
			fail();
		} catch (ServerFault sf) {
		}
	}

	@Test
	public void validate_MailboxAcl_MissingValues() throws SQLException {
		Container c = Container.create("uid", "mailboxacl", "name", "owner");

		ArrayList<AccessControlEntry> accessControlEntries = new ArrayList<AccessControlEntry>();
		accessControlEntries.add(AccessControlEntry.create("bm.lan", Verb.Write));
		accessControlEntries.add(AccessControlEntry.create(UUID.randomUUID().toString(), null));
		try {
			validator.validate(c, accessControlEntries);
			fail();
		} catch (ServerFault sf) {
		}

		accessControlEntries = new ArrayList<AccessControlEntry>();
		accessControlEntries.add(AccessControlEntry.create(null, Verb.Write));
		accessControlEntries.add(AccessControlEntry.create(UUID.randomUUID().toString(), Verb.Read));
		try {
			validator.validate(c, accessControlEntries);
			fail();
		} catch (ServerFault sf) {
		}
	}

}
