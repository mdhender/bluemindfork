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
package net.bluemind.ui.adminconsole.directory.mailshare;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.shared.GWT;

import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.gwtconsoleapp.base.editor.ContainerElement;
import net.bluemind.gwtconsoleapp.base.editor.ModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.ScreenElement;
import net.bluemind.gwtconsoleapp.base.editor.ScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.Tab;
import net.bluemind.gwtconsoleapp.base.editor.TabContainer;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtCompositeScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.mailshare.api.gwt.js.JsMailshare;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.ui.adminconsole.base.Actions;
import net.bluemind.ui.adminconsole.base.ui.BaseDirEntryEditScreen;
import net.bluemind.ui.adminconsole.directory.mailbox.MailboxMaintenance;
import net.bluemind.ui.adminconsole.directory.mailshare.l10n.MailshareConstants;
import net.bluemind.ui.adminconsole.directory.mailshare.l10n.MailshareMenusConstants;
import net.bluemind.ui.mailbox.filter.MailSettingsModelHandler;
import net.bluemind.ui.mailbox.filter.SieveEdit;
import net.bluemind.ui.mailbox.identity.IdentitiesModelHandler;
import net.bluemind.ui.mailbox.identity.IdentityManagement;
import net.bluemind.ui.mailbox.vacation.MailVacationEditor;

public class EditMailshareScreen extends BaseDirEntryEditScreen {
	private static final String TYPE = "bm.ac.EditMailshareScreen";

	private EditMailshareScreen(ScreenRoot screenRoot) {
		super(screenRoot);
		icon.setStyleName("fa fa-2x fa-inbox");
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();
		JsMailshare mailshare = map.get("mailshare").cast();
		title.setInnerHTML(MailshareConstants.INST.editTitle(mailshare.getName()));
	}

	@Override
	public void doLoad(ScreenRoot screenRoot) {
		screenRoot.getState().put("mailshareId", screenRoot.getState().get("entryUid"));
		screenRoot.getState().put("mailboxUid", screenRoot.getState().get("mailshareId"));

		super.doLoad(screenRoot);
	}

	public static void registerType() {
		GwtScreenRoot.registerComposite(TYPE, new IGwtDelegateFactory<IGwtCompositeScreenRoot, ScreenRoot>() {

			@Override
			public IGwtCompositeScreenRoot create(ScreenRoot screenRoot) {
				return new EditMailshareScreen(screenRoot);
			}
		});
	}

	@Override
	protected void doCancel() {
		Actions.get().showWithParams2("directory", null);
	}

	public static ScreenElement screenModel() {

		MailshareMenusConstants c = GWT.create(MailshareMenusConstants.class);
		ScreenRoot screenRoot = ScreenRoot.create("editMailshare", TYPE).cast();
		screenRoot.getHandlers().push(ModelHandler.create(null, MailshareModelHandler.TYPE).readOnly()
				.withRole(BasicRoles.ROLE_MANAGE_MAILSHARE).<ModelHandler>cast());
		screenRoot.getHandlers().push(ModelHandler.create(null, MailSettingsModelHandler.TYPE)
				.withRole(BasicRoles.ROLE_MANAGE_MAILBOX_FILTER).<ModelHandler>cast());
		screenRoot.getHandlers().push(ModelHandler.create(null, IdentitiesModelHandler.TYPE)
				.withRole(BasicRoles.ROLE_MANAGE_MAILBOX_IDENTITIES).<ModelHandler>cast());
		screenRoot.getHandlers().push(ModelHandler.create(null, MailshareMailboxSharingModelHandler.TYPE).<ModelHandler>cast());
		screenRoot.getHandlers().push(ModelHandler.create(null, DomainLoader.TYPE).<ModelHandler>cast());

		JsArray<Tab> tabs = JavaScriptObject.createArray().cast();
		// general
		JsArray<ScreenElement> main = JsArray.createArray().cast();
		main.push(ScreenElement.create(null, MailshareGeneralEditor.TYPE).readOnly()
				.withRole(BasicRoles.ROLE_MANAGE_MAILSHARE));
		main.push(ScreenElement.create(null, MailVacationEditor.TYPE).withRole(BasicRoles.ROLE_MANAGE_MAILBOX_FILTER));
		tabs.push(Tab.create(null, c.basicParametersTab(), ContainerElement.create(null, main)));

		// vcard
		tabs.push(Tab.create(null, c.vcardTab(), ScreenElement.create(null, "bluemind.contact.ui.ContactEditor")
				.withRole(BasicRoles.ROLE_MANAGE_MAILSHARE)));

		tabs.push(Tab.create(null, c.filtersTab(),
				ScreenElement.create(null, SieveEdit.TYPE).withRole(BasicRoles.ROLE_MANAGE_MAILBOX_FILTER)));

		tabs.push(Tab.create(null, c.identitiesTab(), ScreenElement.create(null, IdentityManagement.TYPE)
				.withRole(BasicRoles.ROLE_MANAGE_MAILBOX_IDENTITIES)));

		tabs.push(Tab.create(null, c.mailboxSharingTab(), ScreenElement.create(null, MailshareMailboxSharingEditor.TYPE)));

		// maintenance
		JsArray<ScreenElement> maintenanceContents = JsArray.createArray().cast();
		maintenanceContents
				.push(ScreenElement.create(null, MailboxMaintenance.TYPE).withRole(BasicRoles.ROLE_MANAGE_USER));
		tabs.push(Tab.create(null, c.maintenanceTab(), ContainerElement.create(null, maintenanceContents)));

		TabContainer tab = TabContainer.create("editMailshareTabs", tabs);
		screenRoot.setContent(tab);
		return screenRoot;
	}

}
