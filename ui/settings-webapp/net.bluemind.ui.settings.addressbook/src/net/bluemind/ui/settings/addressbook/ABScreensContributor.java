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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;

import net.bluemind.gwtconsoleapp.base.editor.ScreenElement;
import net.bluemind.gwtconsoleapp.base.editor.ScreenElementContribution;
import net.bluemind.gwtconsoleapp.base.editor.ScreenElementContributorUnwrapper;
import net.bluemind.gwtconsoleapp.base.editor.Tab;
import net.bluemind.gwtconsoleapp.base.editor.TabContainer;
import net.bluemind.ui.common.client.forms.Ajax;
import net.bluemind.ui.gwtuser.client.UserBooksSharingsEditor;
import net.bluemind.ui.gwtuser.client.UserBooksSubscriptionEditor;
import net.bluemind.ui.gwtuser.client.UserBooksSubscriptionModelHandler;
import net.bluemind.ui.gwtuser.client.UserSettingsBooksSharingModelHandler;

public class ABScreensContributor implements ScreenElementContributorUnwrapper {
	private static final AddressBookMessages messages = GWT.create(AddressBookMessages.class);

	@Override
	public JsArray<ScreenElementContribution> contribution() {

		JsArray<Tab> tabs = JsArray.createArray().cast();

		tabs.push(Tab.create(null, messages.tabFolders(), ScreenElement.create(null, "bm.settings.MyBooksEditor")));

		ScreenElement contribution = TabContainer.create("/contact/", tabs);
		JsArray<ScreenElementContribution> contribs = JsArray.createArray().cast();
		contribs.push(ScreenElementContribution.create("root", "childrens", contribution));

		if ("FULL".equals(Ajax.getAccountType())) {
			tabs.push(Tab.create(null, messages.tabSubscriptions(),
					ScreenElement.create(null, UserBooksSubscriptionEditor.TYPE)));

			tabs.push(
					Tab.create(null, messages.tabSharings(), ScreenElement.create(null, UserBooksSharingsEditor.TYPE)));

			contribs.push(ScreenElementContribution.create("base", "modelHandlers",
					ScreenElement.create(null, UserSettingsBooksSharingModelHandler.TYPE)));

			contribs.push(ScreenElementContribution.create("base", "modelHandlers",
					ScreenElement.create(null, UserBooksSubscriptionModelHandler.TYPE)));
		}
		return contribs;
	}

}
