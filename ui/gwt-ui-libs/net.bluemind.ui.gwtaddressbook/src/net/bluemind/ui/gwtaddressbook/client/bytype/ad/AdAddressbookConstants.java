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
package net.bluemind.ui.gwtaddressbook.client.bytype.ad;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.Messages;

public interface AdAddressbookConstants extends Messages {
	public static final AdAddressbookConstants INST = GWT.create(AdAddressbookConstants.class);

	public String emptyLabel();

	public String label();

	String entryUUID();

	String adHostname();

	String adProtocol();

	String adBaseDn();

	String adLoginDn();

	String adLoginPw();

	String adUserFilter();

	String adConnTest();

	String allCertificate();

	String errorInvalidHostname();

	String errorInvalidCredential();

	String errorInvalidDn();

	String reset();

	String confirmReset();

	String resetOk();

	String launchSync();

	String lastSync();

}
