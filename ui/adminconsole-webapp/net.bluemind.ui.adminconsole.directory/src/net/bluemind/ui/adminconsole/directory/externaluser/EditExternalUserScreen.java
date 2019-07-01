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
package net.bluemind.ui.adminconsole.directory.externaluser;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.uibinder.client.UiFactory;

import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.externaluser.api.gwt.js.JsExternalUser;
import net.bluemind.gwtconsoleapp.base.editor.ModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.ScreenElement;
import net.bluemind.gwtconsoleapp.base.editor.ScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.Tab;
import net.bluemind.gwtconsoleapp.base.editor.TabContainer;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtCompositeScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.ui.adminconsole.base.Actions;
import net.bluemind.ui.adminconsole.base.orgunit.OrgUnitsAdministratorModelHandler;
import net.bluemind.ui.adminconsole.base.ui.BaseEditScreen;
import net.bluemind.ui.adminconsole.directory.externaluser.l10n.ExternalUserConstants;
import net.bluemind.ui.adminconsole.directory.externaluser.l10n.ExternalUserMenusConstants;
import net.bluemind.ui.adminconsole.directory.mailshare.DomainLoader;

public class EditExternalUserScreen extends BaseEditScreen {

	public static final String TYPE = "bm.ac.EditExternalUserScreen";

	private EditExternalUserScreen(ScreenRoot screenRoot) {
		super(screenRoot);
		icon.setStyleName("fa fa-2x fa-user-secret");
	}

	@UiFactory
	ExternalUserConstants getTexts() {
		return ExternalUserConstants.INST;
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();
		JsExternalUser externalUser = map.get("externaluser").cast();
		title.setInnerText(ExternalUserConstants.INST
				.editTitle(externalUser.getContactInfos().getIdentification().getFormatedName().getValue()));
	}

	public static void registerType() {
		GwtScreenRoot.registerComposite("bm.ac.EditExternalUserScreen",
				new IGwtDelegateFactory<IGwtCompositeScreenRoot, ScreenRoot>() {

					@Override
					public IGwtCompositeScreenRoot create(
							ScreenRoot screenRoot) {
						return new EditExternalUserScreen(screenRoot);
					}
				});
	}

	@Override
	protected void doCancel() {
		Actions.get().showWithParams2("directory", null);
	}

	@Override
	public void doLoad(ScreenRoot screenRoot) {
		screenRoot.getState().put("externalUserId",
				screenRoot.getState().get("entryUid"));
		super.doLoad(screenRoot);
	}

	public static ScreenElement screenModel() {
		ExternalUserMenusConstants c = GWT
				.create(ExternalUserMenusConstants.class);

		ScreenRoot screenRoot = ScreenRoot.create("editExternalUser", TYPE)
				.cast();

		screenRoot.getHandlers()
				.push(ModelHandler.create(null, ExternalUserModelHandler.TYPE)
						.readOnly()
						.withRole(BasicRoles.ROLE_MANAGE_EXTERNAL_USER)
						.<ModelHandler>cast());
		screenRoot.getHandlers().push(ModelHandler
				.create(null, DomainLoader.TYPE).<ModelHandler>cast());
		screenRoot.getHandlers()
				.push(ModelHandler
						.create(null, OrgUnitsAdministratorModelHandler.TYPE)
						.withRole(BasicRoles.ROLE_MANAGE_EXTERNAL_USER)
						.<ModelHandler>cast());

		JsArray<Tab> tabs = JavaScriptObject.createArray().cast();

		tabs.push(Tab.create(null, c.generalTab(),
				ScreenElement.create(null, EditExternalUser.TYPE).readOnly()
						.withRole(BasicRoles.ROLE_MANAGE_EXTERNAL_USER)));

		tabs.push(Tab.create(null, c.vcardTab(),
				ScreenElement.create(null, "bluemind.contact.ui.ContactEditor")
						.withRole(BasicRoles.ROLE_MANAGE_EXTERNAL_USER)));

		TabContainer tab = TabContainer.create("editExternalUserTabs", tabs);
		screenRoot.setContent(tab);
		return screenRoot;
	}

}
