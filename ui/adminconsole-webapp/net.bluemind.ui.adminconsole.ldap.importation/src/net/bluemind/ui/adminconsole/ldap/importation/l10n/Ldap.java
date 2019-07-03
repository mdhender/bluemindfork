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
package net.bluemind.ui.adminconsole.ldap.importation.l10n;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.ConstantsWithLookup;

public interface Ldap extends ConstantsWithLookup {

	public static final Ldap INST = GWT.create(Ldap.class);

	String tabName();

	String ldap();

	String ldapEnableImport();

	String ldapHostname();

	String ldapProtocol();

	String ldapBaseDn();

	String ldapLoginDn();

	String ldapLoginPw();

	String ldapUserFilter();

	String ldapGroupFilter();

	String ldapExternalId();

	String ldapConnTest();

	String ldapSplitDomainGroup();

	String ldapStartGlobal();

	String ldapStartIncremental();

	String lastSuccessfulSync();

	String unknownLastSync();

	String lastSyncStatus();

	String buttonTipDisbledOnChange();

	String fail();

	String testSuccess();

	String incrementalStartSuccess();

	String incrementalStartFail();

	String globalStartSuccess();

	String globalStartFail();

	String allCertificate();
}
