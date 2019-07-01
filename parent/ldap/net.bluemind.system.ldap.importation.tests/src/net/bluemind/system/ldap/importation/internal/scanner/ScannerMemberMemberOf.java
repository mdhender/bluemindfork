package net.bluemind.system.ldap.importation.internal.scanner;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.junit.Test;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.group.api.Group;
import net.bluemind.group.api.Member;
import net.bluemind.system.importation.commons.scanner.ImportLogger;
import net.bluemind.system.ldap.importation.internal.tools.LdapParameters;
import net.bluemind.system.ldap.tests.helpers.LdapDockerTestHelper;

public abstract class ScannerMemberMemberOf extends ScannerCommon {
	@Test
	public void addGroupMember()
			throws LdapInvalidDnException, ServerFault, LdapException, CursorException, IOException {
		CoreServicesTest coreService = new CoreServicesTest();

		ItemValue<Group> group00 = getExistingGroup("cn=grptest00," + LdapDockerTestHelper.LDAP_ROOT_DN);
		coreService.groups.put(group00.uid, group00);
		ItemValue<Group> group01 = getExistingGroup("cn=grptest01," + LdapDockerTestHelper.LDAP_ROOT_DN);
		coreService.groups.put(group01.uid, group01);

		ImportLogger importLogger = getImportLogger();
		scanLdap(importLogger, coreService, LdapParameters.build(getDomain(), Collections.<String, String>emptyMap()));

		assertEquals(1, coreService.groupMembersToAdd.size());
		assertEquals(group01.uid, coreService.groupMembersToAdd.keySet().iterator().next());

		List<Member> members = coreService.groupMembersToAdd.values().iterator().next();
		assertEquals(1, members.size());
		assertEquals(Member.Type.group, members.get(0).type);
		assertEquals(group00.uid, members.get(0).uid);
	}

	@Test
	public void removeGroupMember()
			throws LdapInvalidDnException, ServerFault, LdapException, CursorException, IOException {
		CoreServicesTest coreService = new CoreServicesTest();

		ItemValue<Group> group00 = getExistingGroup("cn=grptest00," + LdapDockerTestHelper.LDAP_ROOT_DN);
		coreService.groups.put(group00.uid, group00);
		ItemValue<Group> group01 = getExistingGroup("cn=grptest01," + LdapDockerTestHelper.LDAP_ROOT_DN);
		coreService.groups.put(group01.uid, group01);

		coreService.addGroupToGroup(group00.uid, group01);

		ImportLogger importLogger = getImportLogger();
		scanLdap(importLogger, coreService, LdapParameters.build(getDomain(), Collections.<String, String>emptyMap()));

		assertEquals(1, coreService.groupMembersToRemove.size());
		assertEquals(group01.uid, coreService.groupMembersToRemove.keySet().iterator().next());

		List<Member> members = coreService.groupMembersToRemove.values().iterator().next();
		assertEquals(1, members.size());
		assertEquals(Member.Type.group, members.get(0).type);
		assertEquals(group00.uid, members.get(0).uid);
	}
}
