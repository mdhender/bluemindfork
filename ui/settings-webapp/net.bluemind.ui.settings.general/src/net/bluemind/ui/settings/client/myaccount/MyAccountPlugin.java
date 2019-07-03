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
package net.bluemind.ui.settings.client.myaccount;

import net.bluemind.gwtconsoleapp.base.editor.ScreenElementContributor;
import net.bluemind.gwtconsoleapp.base.menus.MenuContributor;
import net.bluemind.ui.gwttag.client.TagsEditor;
import net.bluemind.ui.gwttag.client.UserTagsModelHandler;
import net.bluemind.ui.gwtuser.client.UserSettingsEditor;
import net.bluemind.ui.gwtuser.client.UserSettingsModelHandler;
import net.bluemind.ui.settings.client.forms.PasswordEdit;
import net.bluemind.ui.settings.client.myaccount.external.ExternalAccountModelHandler;
import net.bluemind.ui.settings.client.myaccount.external.ExternalAccountsWidget;

public class MyAccountPlugin {

	public static void install() {

		UserSettingsModelHandler.registerType();
		UserSettingsEditor.registerType();

		MyAccountAdvancedPartWidget.registerType();
		TagsEditor.registerType();
		UserTagsModelHandler.registerType();
		PasswordEdit.registerType();
		MyAccountAdvancedPartWidget.registerType();
		ExternalAccountModelHandler.registerType();
		ExternalAccountsWidget.registerType();

		MenuContributor.exportAsfunction("gwtSettingsMyAccountMenusContributor",
				MenuContributor.create(new MyAccountMenusContributor()));
		ScreenElementContributor.exportAsfunction("gwtSettingsMyAccountScreensContributor",
				ScreenElementContributor.create(new MyAccountScreensContributor()));

	}
}
