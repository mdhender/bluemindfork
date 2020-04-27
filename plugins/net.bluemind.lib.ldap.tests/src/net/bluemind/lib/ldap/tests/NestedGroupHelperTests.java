/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.lib.ldap.tests;

import java.io.IOException;
import java.util.Set;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import net.bluemind.lib.ldap.GroupMemberAttribute;
import net.bluemind.lib.ldap.LdapConProxy;
import net.bluemind.lib.ldap.NestedGroupHelper;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.pool.impl.docker.DockerContainer;
import net.bluemind.system.ldap.tests.helpers.LdapDockerTestHelper;

public class NestedGroupHelperTests {
	private static final String groupFilter = "(objectClass=bmGroup)";

	@Rule
	public TestName testName = new TestName();

	@BeforeClass
	public static void beforeClass() {
		LdapDockerTestHelper.initLdapServer();
	}

	@Before
	public void before() throws Exception {
		LdapDockerTestHelper.initLdapTree(this.getClass(), testName);
	}

	@Test
	public void noMember() throws IOException, LdapException {
		LdapConnectionConfig config = new LdapConnectionConfig();
		config.setLdapHost(new BmConfIni().get(DockerContainer.LDAP.getName()));
		config.setLdapPort(389);

		try (LdapConProxy ldapCon = new LdapConProxy(config)) {
			Entry group = ldapCon.lookup("cn=grptest00,dc=local");
			Assert.assertNotNull(group);

			NestedGroupHelper ngh = new NestedGroupHelper(ldapCon, new Dn("dc=local"), GroupMemberAttribute.member,
					groupFilter, "uid", "entryuuid");
			Assert.assertTrue(ngh.getUserMembers(group).isEmpty());

			ngh = new NestedGroupHelper(ldapCon, new Dn("dc=local"), GroupMemberAttribute.memberUid, groupFilter, "uid",
					"entryuuid");
			Assert.assertTrue(ngh.getUserMembers(group).isEmpty());
		}
	}

	@Test
	public void member() throws IOException, LdapException {
		LdapConnectionConfig config = new LdapConnectionConfig();
		config.setLdapHost(new BmConfIni().get(DockerContainer.LDAP.getName()));
		config.setLdapPort(389);

		// Check grptest00
		try (LdapConProxy ldapCon = new LdapConProxy(config)) {
			Entry group = ldapCon.lookup("cn=grptest00,dc=local");
			Assert.assertNotNull(group);

			NestedGroupHelper ngh = new NestedGroupHelper(ldapCon, new Dn("dc=local"), GroupMemberAttribute.member,
					groupFilter, "uid", "entryuuid");
			Set<String> members = ngh.getUserMembers(group);
			Assert.assertEquals(1, members.size());

			Entry user = ldapCon.lookup("uid=user00,dc=local", "entryuuid");
			Assert.assertTrue(members.contains(user.get("entryuuid").get().toString()));

			ngh = new NestedGroupHelper(ldapCon, new Dn("dc=local"), GroupMemberAttribute.memberUid, groupFilter, "uid",
					"entryuuid");
			Assert.assertTrue(ngh.getUserMembers(group).isEmpty());
		}

		// Check grptest01
		try (LdapConProxy ldapCon = new LdapConProxy(config)) {
			Entry group = ldapCon.lookup("cn=grptest01,dc=local");
			Assert.assertNotNull(group);

			NestedGroupHelper ngh = new NestedGroupHelper(ldapCon, new Dn("dc=local"), GroupMemberAttribute.member,
					groupFilter, "uid", "entryuuid");
			Set<String> members = ngh.getUserMembers(group);
			Assert.assertEquals(2, members.size());

			Entry user = ldapCon.lookup("uid=user00,dc=local", "entryuuid");
			Assert.assertTrue(members.contains(user.get("entryuuid").get().toString()));
			user = ldapCon.lookup("uid=user01,dc=local", "entryuuid");
			Assert.assertTrue(members.contains(user.get("entryuuid").get().toString()));

			ngh = new NestedGroupHelper(ldapCon, new Dn("dc=local"), GroupMemberAttribute.memberUid, groupFilter, "uid",
					"entryuuid");
			Assert.assertTrue(ngh.getUserMembers(group).isEmpty());
		}

		// Check grptest03
		try (LdapConProxy ldapCon = new LdapConProxy(config)) {
			Entry group = ldapCon.lookup("cn=grptest03,dc=local");
			Assert.assertNotNull(group);

			NestedGroupHelper ngh = new NestedGroupHelper(ldapCon, new Dn("dc=local"), GroupMemberAttribute.member,
					groupFilter, "uid", "entryuuid");
			Set<String> members = ngh.getUserMembers(group);
			Assert.assertEquals(2, members.size());

			Entry user = ldapCon.lookup("uid=user00,dc=local", "entryuuid");
			Assert.assertTrue(members.contains(user.get("entryuuid").get().toString()));
			user = ldapCon.lookup("uid=user01,dc=local", "entryuuid");
			Assert.assertTrue(members.contains(user.get("entryuuid").get().toString()));

			ngh = new NestedGroupHelper(ldapCon, new Dn("dc=local"), GroupMemberAttribute.memberUid, groupFilter, "uid",
					"entryuuid");
			Assert.assertTrue(ngh.getUserMembers(group).isEmpty());
		}
	}

	@Test
	public void memberUid() throws IOException, LdapException {
		LdapConnectionConfig config = new LdapConnectionConfig();
		config.setLdapHost(new BmConfIni().get(DockerContainer.LDAP.getName()));
		config.setLdapPort(389);

		// Check grptest00
		try (LdapConProxy ldapCon = new LdapConProxy(config)) {
			Entry group = ldapCon.lookup("cn=grptest00,dc=local");
			Assert.assertNotNull(group);

			NestedGroupHelper ngh = new NestedGroupHelper(ldapCon, new Dn("dc=local"), GroupMemberAttribute.memberUid,
					groupFilter, "uid", "entryuuid");
			Set<String> members = ngh.getUserMembers(group);
			Assert.assertEquals(2, members.size());

			Entry user = ldapCon.lookup("uid=user00,dc=local", "entryuuid");
			Assert.assertTrue(members.contains(user.get("entryuuid").get().toString()));
			user = ldapCon.lookup("uid=user01,dc=local", "entryuuid");
			Assert.assertTrue(members.contains(user.get("entryuuid").get().toString()));

			ngh = new NestedGroupHelper(ldapCon, new Dn("dc=local"), GroupMemberAttribute.member, groupFilter, "uid",
					"entryuuid");
			Assert.assertTrue(ngh.getUserMembers(group).isEmpty());
		}
	}

	@Test
	public void memberUnknownDn() throws IOException, LdapException {
		LdapConnectionConfig config = new LdapConnectionConfig();
		config.setLdapHost(new BmConfIni().get(DockerContainer.LDAP.getName()));
		config.setLdapPort(389);

		// Check grptest00
		try (LdapConProxy ldapCon = new LdapConProxy(config)) {
			Entry group = ldapCon.lookup("cn=grptest00,dc=local");
			Assert.assertNotNull(group);

			NestedGroupHelper ngh = new NestedGroupHelper(ldapCon, new Dn("dc=local"), GroupMemberAttribute.member,
					groupFilter, "uid", "entryuuid");
			Set<String> members = ngh.getUserMembers(group);
			Assert.assertEquals(2, members.size());

			Entry user = ldapCon.lookup("uid=user00,dc=local", "entryuuid");
			Assert.assertTrue(members.contains(user.get("entryuuid").get().toString()));
			user = ldapCon.lookup("uid=user01,dc=local", "entryuuid");
			Assert.assertTrue(members.contains(user.get("entryuuid").get().toString()));
		}
	}

	@Test
	public void memberUidUnknownUid() throws IOException, LdapException {
		LdapConnectionConfig config = new LdapConnectionConfig();
		config.setLdapHost(new BmConfIni().get(DockerContainer.LDAP.getName()));
		config.setLdapPort(389);

		// Check grptest00
		try (LdapConProxy ldapCon = new LdapConProxy(config)) {
			Entry group = ldapCon.lookup("cn=grptest00,dc=local");
			Assert.assertNotNull(group);

			NestedGroupHelper ngh = new NestedGroupHelper(ldapCon, new Dn("dc=local"), GroupMemberAttribute.memberUid,
					groupFilter, "uid", "entryuuid");
			Set<String> members = ngh.getUserMembers(group);
			Assert.assertEquals(2, members.size());

			Entry user = ldapCon.lookup("uid=user00,dc=local", "entryuuid");
			Assert.assertTrue(members.contains(user.get("entryuuid").get().toString()));
			user = ldapCon.lookup("uid=user01,dc=local", "entryuuid");
			Assert.assertTrue(members.contains(user.get("entryuuid").get().toString()));
		}
	}
}
