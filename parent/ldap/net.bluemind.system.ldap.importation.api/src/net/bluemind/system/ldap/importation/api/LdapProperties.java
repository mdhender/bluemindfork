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
package net.bluemind.system.ldap.importation.api;

public enum LdapProperties {
	import_ldap_enabled("false"), import_ldap_hostname(null), import_ldap_protocol("plain"),
	import_ldap_accept_certificate("false"), import_ldap_login_dn(null), import_ldap_password(null),
	import_ldap_base_dn(null), import_ldap_ext_id_attribute("entryUUID"), import_ldap_relay_mailbox_group(null),
	import_ldap_lastupdate(null), import_ldap_user_filter("(objectClass=inetOrgPerson)"),
	import_ldap_group_filter("(|(objectClass=posixGroup)(objectClass=groupOfNames))");

	private String defaultValue;

	private LdapProperties(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public String getDefaultValue() {
		return this.defaultValue;
	}
}
