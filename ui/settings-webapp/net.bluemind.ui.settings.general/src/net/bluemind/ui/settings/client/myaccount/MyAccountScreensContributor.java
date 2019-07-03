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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;

import net.bluemind.gwtconsoleapp.base.editor.ContainerElement;
import net.bluemind.gwtconsoleapp.base.editor.ModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.ScreenElement;
import net.bluemind.gwtconsoleapp.base.editor.ScreenElementContribution;
import net.bluemind.gwtconsoleapp.base.editor.ScreenElementContributorUnwrapper;
import net.bluemind.gwtconsoleapp.base.editor.Tab;
import net.bluemind.gwtconsoleapp.base.editor.TabContainer;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.ui.gwttag.client.TagsEditor;
import net.bluemind.ui.gwttag.client.UserTagsModelHandler;
import net.bluemind.ui.gwtuser.client.UserSettingsEditor;
import net.bluemind.ui.gwtuser.client.UserSettingsModelHandler;
import net.bluemind.ui.settings.client.forms.PasswordEdit;
import net.bluemind.ui.settings.client.myaccount.external.ExternalAccountModelHandler;
import net.bluemind.ui.settings.client.myaccount.external.ExternalAccountsWidget;

public class MyAccountScreensContributor implements ScreenElementContributorUnwrapper {

	public static final AccountMessages messages = GWT.create(AccountMessages.class);

	@Override
	public JsArray<ScreenElementContribution> contribution() {

		// FIXME we should only check BasicRoles.ROLE_MANAGE_USER_SETTINGS
		// and ROLE_SELF_CHANGE_SETTINGS shoul be "transformed" to
		// ROLE_MANAGE_USER_SETTINGS
		// when accessing "self"
		JsArray<ScreenElement> userGeneralElts = JsArray.createArray().cast();
		userGeneralElts.push(ScreenElement.create(null, UserSettingsEditor.TYPE).readOnly()
				.withRoles(BasicRoles.ROLE_SELF_CHANGE_SETTINGS, BasicRoles.ROLE_MANAGE_USER_SETTINGS));
		userGeneralElts
				.push(ScreenElement.create(null, PasswordEdit.TYPE).withRole(BasicRoles.ROLE_SELF_CHANGE_PASSWORD));
		ContainerElement userGeneralContainer = ContainerElement.create("userGenralContainer", userGeneralElts);

		JsArray<Tab> tabs = JsArray.createArray().cast();
		tabs.push(Tab.create(null, messages.tabGeneral(), userGeneralContainer));
		tabs.push(Tab.create(null, messages.tabTags(), ScreenElement.create(null, TagsEditor.TYPE)));
		tabs.push(
				Tab.create(null, messages.tabAdvanced(), ScreenElement.create(null, MyAccountAdvancedPartWidget.TYPE)));
		tabs.push(Tab.create(null, messages.tabExternalAccounts(), ScreenElement
				.create(null, ExternalAccountsWidget.TYPE).withRole(BasicRoles.ROLE_SELF_MANAGE_EXTERNAL_ACCOUNT)));

		ScreenElement contribution = TabContainer.create("myAccount", tabs);
		ScreenElement useModelHandler = ScreenElement.create(null, UserSettingsModelHandler.TYPE).readOnly()
				.withRoles(BasicRoles.ROLE_SELF_CHANGE_SETTINGS, BasicRoles.ROLE_MANAGE_USER_SETTINGS);
		ScreenElement externalAccountModelHandler = ModelHandler.create(null, ExternalAccountModelHandler.TYPE)
				.withRole(BasicRoles.ROLE_SELF_MANAGE_EXTERNAL_ACCOUNT);

		JsArray<ScreenElementContribution> contribs = JsArray.createArray().cast();
		contribs.push(ScreenElementContribution.create("root", "childrens", contribution));
		contribs.push(ScreenElementContribution.create("base", "modelHandlers", useModelHandler));
		contribs.push(ScreenElementContribution.create("base", "modelHandlers", externalAccountModelHandler));
		contribs.push(ScreenElementContribution.create("base", "modelHandlers",
				ModelHandler.create(null, UserTagsModelHandler.TYPE)));
		return contribs;
	}

}