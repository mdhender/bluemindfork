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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;

import net.bluemind.gwtconsoleapp.base.editor.ModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.ScreenElement;
import net.bluemind.gwtconsoleapp.base.editor.ScreenElementContribution;
import net.bluemind.gwtconsoleapp.base.editor.ScreenElementContributorUnwrapper;
import net.bluemind.gwtconsoleapp.base.editor.Tab;
import net.bluemind.gwtconsoleapp.base.editor.TabContainer;
import net.bluemind.ui.gwtuser.client.UserSettingsTodolistsSharingModelHandler;
import net.bluemind.ui.gwtuser.client.UserTodolistsSharingsEditor;
import net.bluemind.ui.gwtuser.client.UserTodolistsSubscriptionModelHandler;
import net.bluemind.ui.gwtuser.client.UserTodolistsSubscriptionsEditor;

public class TaskScreensContributor implements ScreenElementContributorUnwrapper {
	private static final TaskMessages messages = GWT.create(TaskMessages.class);

	@Override
	public JsArray<ScreenElementContribution> contribution() {

		JsArray<Tab> tabs = JsArray.createArray().cast();

		tabs.push(Tab.create(null, messages.tabMyLists(), ScreenElement.create(null, MyTodoListsPartWidget.TYPE)));

		tabs.push(Tab.create(null, messages.tabSubscriptions(),
				ScreenElement.create(null, UserTodolistsSubscriptionsEditor.TYPE)));

		tabs.push(
				Tab.create(null, messages.tabSharings(), ScreenElement.create(null, UserTodolistsSharingsEditor.TYPE)));

		ScreenElement contribution = TabContainer.create("/task/", tabs);
		JsArray<ScreenElementContribution> contribs = JsArray.createArray().cast();
		contribs.push(ScreenElementContribution.create("root", "childrens", contribution));

		contribs.push(ScreenElementContribution.create("base", "modelHandlers",
				ScreenElement.create(null, UserTodolistsSubscriptionModelHandler.TYPE)));
		contribs.push(ScreenElementContribution.create("base", "modelHandlers",
				ModelHandler.create(null, UserSettingsTodolistsSharingModelHandler.TYPE)));

		return contribs;
	}

}
