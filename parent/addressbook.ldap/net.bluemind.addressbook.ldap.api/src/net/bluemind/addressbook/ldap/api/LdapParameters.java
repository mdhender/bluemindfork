/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.addressbook.ldap.api;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class LdapParameters {

	@BMApi(version = "3")
	public static enum DirectoryType {
		ldap, ad
	}

	public static final String AD_UUID = "objectGuid";

	private static final String AD_MODIFYTIMESTAMP_ATTR = "whenChanged";
	private static final String LDAP_MODIFYTIMESTAMP_ATTR = "modifyTimestamp";

	public DirectoryType type;

	public String hostname;

	public String protocol;

	public String baseDn;

	public String loginDn;

	public String loginPw;

	public String filter;

	public boolean allCertificate;

	public String entryUUID;

	public String modifyTimeStampAttr;

	public static LdapParameters create(DirectoryType type, String hostname, String protocol, boolean allCertificate,
			String baseDn, String loginDn, String loginPw, String filter, String entryUUID) {

		LdapParameters lp = new LdapParameters();
		lp.type = type;
		lp.hostname = hostname;
		lp.protocol = protocol;
		lp.allCertificate = allCertificate;
		lp.baseDn = baseDn;
		lp.loginDn = loginDn;
		lp.loginPw = loginPw;
		lp.filter = filter;
		if (type == DirectoryType.ad) {
			lp.entryUUID = AD_UUID;
			lp.modifyTimeStampAttr = AD_MODIFYTIMESTAMP_ATTR;
		} else {
			lp.entryUUID = entryUUID;
			lp.modifyTimeStampAttr = LDAP_MODIFYTIMESTAMP_ATTR;
		}

		return lp;

	}

}
