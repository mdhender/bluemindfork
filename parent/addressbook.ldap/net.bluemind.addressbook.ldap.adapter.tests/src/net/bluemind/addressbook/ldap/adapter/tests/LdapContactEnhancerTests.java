package net.bluemind.addressbook.ldap.adapter.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.ldif.LdifEntry;
import org.junit.Test;

import net.bluemind.addressbook.ldap.adapter.InetOrgPersonAdapter;
import net.bluemind.addressbook.ldap.adapter.LdapContact;
import net.bluemind.addressbook.ldap.api.LdapParameters;

public class LdapContactEnhancerTests {
	@Test
	public void testLdapContactEnhancer() throws LdapException {
		LdapContact lc = InetOrgPersonAdapter.getVCard(new LdifEntry().getEntry(), LdapParameters.DirectoryType.ldap,
				"uid");
		assertNull(lc.uid);

		LdifEntry ldif = new LdifEntry();
		ldif.addAttribute("enhancerattr", "yeahUid");
		lc = InetOrgPersonAdapter.getVCard(ldif.getEntry(), LdapParameters.DirectoryType.ldap, "uid");
		assertEquals("yeahUid", lc.uid);
	}
}
