package net.bluemind.addressbook.ldap.adapter.enhancer;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapInvalidAttributeValueException;

import net.bluemind.addressbook.ldap.adapter.LdapContact;

public interface ILdapContactEnhancer {
	void enhanceLdapContact(Entry entry, LdapContact lc) throws LdapInvalidAttributeValueException;
}
