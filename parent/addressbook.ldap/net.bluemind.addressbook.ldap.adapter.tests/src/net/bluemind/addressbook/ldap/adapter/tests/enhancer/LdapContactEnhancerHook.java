package net.bluemind.addressbook.ldap.adapter.tests.enhancer;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapInvalidAttributeValueException;

import net.bluemind.addressbook.ldap.adapter.LdapContact;
import net.bluemind.addressbook.ldap.adapter.enhancer.ILdapContactEnhancer;

public class LdapContactEnhancerHook implements ILdapContactEnhancer {
	@Override
	public void enhanceLdapContact(Entry entry, LdapContact lc) throws LdapInvalidAttributeValueException {
		if (entry.containsAttribute("enhancerattr")) {
			lc.uid = entry.get("enhancerattr").getString();
		}
	}
}
