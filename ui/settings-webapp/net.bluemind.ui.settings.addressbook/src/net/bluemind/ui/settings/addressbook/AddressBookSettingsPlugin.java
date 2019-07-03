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
package net.bluemind.ui.settings.addressbook;

import net.bluemind.gwtconsoleapp.base.editor.ScreenElementContributor;
import net.bluemind.gwtconsoleapp.base.menus.MenuContributor;
import net.bluemind.ui.gwtaddressbook.client.GwtAddressbookBundle;
import net.bluemind.ui.gwtuser.client.UserBooksSharingsEditor;
import net.bluemind.ui.gwtuser.client.UserBooksSubscriptionEditor;
import net.bluemind.ui.gwtuser.client.UserBooksSubscriptionModelHandler;
import net.bluemind.ui.gwtuser.client.UserSettingsBooksSharingModelHandler;

public class AddressBookSettingsPlugin {

	public static void install() {

		MenuContributor.exportAsfunction("gwtSettingsAddressBookMenusContributor",
				MenuContributor.create(new ABMenusContributor()));

		ScreenElementContributor.exportAsfunction("gwtSettingsAddressBookScreensContributor",
				ScreenElementContributor.create(new ABScreensContributor()));

		MyBooksPartWidget.registerType();
		UserBooksSharingsEditor.registerType();
		UserBooksSubscriptionEditor.registerType();
		UserSettingsBooksSharingModelHandler.registerType();
		UserBooksSubscriptionModelHandler.registerType();

		GwtAddressbookBundle.register();
	}
}
