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
package net.bluemind.ui.gwtaddressbook.client;

import net.bluemind.ui.gwtaddressbook.client.bytype.ad.AdAddressbookActions;
import net.bluemind.ui.gwtaddressbook.client.bytype.ad.AdAddressbookCreationWidget;
import net.bluemind.ui.gwtaddressbook.client.bytype.internal.InternalAddressbookActions;
import net.bluemind.ui.gwtaddressbook.client.bytype.internal.InternalAddressbookCreationWidget;
import net.bluemind.ui.gwtaddressbook.client.bytype.ldap.LdapAddressbookActions;
import net.bluemind.ui.gwtaddressbook.client.bytype.ldap.LdapAddressbookCreationWidget;

public class GwtAddressbookBundle {

	public static void register() {
		InternalAddressbookCreationWidget.registerType();
		InternalAddressbookActions.registerType();

		LdapAddressbookCreationWidget.registerType();
		LdapAddressbookActions.registerType();

		AdAddressbookCreationWidget.registerType();
		AdAddressbookActions.registerType();
	}

}
