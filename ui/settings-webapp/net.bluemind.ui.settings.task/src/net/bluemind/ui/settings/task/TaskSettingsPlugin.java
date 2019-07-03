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
package net.bluemind.ui.settings.task;

import net.bluemind.gwtconsoleapp.base.editor.ScreenElementContributor;
import net.bluemind.gwtconsoleapp.base.menus.MenuContributor;
import net.bluemind.ui.gwtuser.client.UserSettingsTodolistsSharingModelHandler;
import net.bluemind.ui.gwtuser.client.UserTodolistsSharingModelHandler;
import net.bluemind.ui.gwtuser.client.UserTodolistsSharingsEditor;
import net.bluemind.ui.gwtuser.client.UserTodolistsSubscriptionModelHandler;
import net.bluemind.ui.gwtuser.client.UserTodolistsSubscriptionsEditor;

public class TaskSettingsPlugin {

	public static void install() {

		MenuContributor.exportAsfunction("gwtSettingsTaskMenusContributor",
				MenuContributor.create(new TaskMenusContributor()));

		ScreenElementContributor.exportAsfunction("gwtSettingsTaskScreensContributor",
				ScreenElementContributor.create(new TaskScreensContributor()));

		UserTodolistsSubscriptionsEditor.registerType();
		UserTodolistsSharingsEditor.registerType();
		UserTodolistsSubscriptionModelHandler.registerType();
		UserTodolistsSharingModelHandler.registerType();
		UserSettingsTodolistsSharingModelHandler.registerType();
		MyTodoListsPartWidget.registerType();
	}
}
