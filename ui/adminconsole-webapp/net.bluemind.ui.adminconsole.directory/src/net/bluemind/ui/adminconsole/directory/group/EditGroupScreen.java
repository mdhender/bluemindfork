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
package net.bluemind.ui.adminconsole.directory.group;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.uibinder.client.UiFactory;

import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.group.api.gwt.js.JsGroup;
import net.bluemind.gwtconsoleapp.base.editor.ContainerElement;
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
import net.bluemind.ui.adminconsole.base.orgunit.OrgUnitsAdministratorEditor;
import net.bluemind.ui.adminconsole.base.orgunit.OrgUnitsAdministratorModelHandler;
import net.bluemind.ui.adminconsole.base.ui.BaseEditScreen;
import net.bluemind.ui.adminconsole.directory.commons.ExternalIdEditor;
import net.bluemind.ui.adminconsole.directory.group.l10n.GroupConstants;
import net.bluemind.ui.adminconsole.directory.group.l10n.GroupMenusConstants;
import net.bluemind.ui.adminconsole.directory.mailbox.MailboxMaintenance;
import net.bluemind.ui.adminconsole.directory.mailshare.DomainLoader;
import net.bluemind.ui.gwtrole.client.GroupRolesModelHandler;

public class EditGroupScreen extends BaseEditScreen {

	public static final String TYPE = "bm.ac.EditGroupScreen";

	private EditGroupScreen(ScreenRoot screenRoot) {
		super(screenRoot);
		icon.setStyleName("fa fa-2x fa-users");
	}

	@UiFactory
	GroupConstants getTexts() {
		return GroupConstants.INST;
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();
		JsGroup group = map.get("group").cast();
		title.setInnerText(GroupConstants.INST.editTitle(group.getName()));
	}

	public static void registerType() {
		GwtScreenRoot.registerComposite("bm.ac.EditGroupScreen",
				new IGwtDelegateFactory<IGwtCompositeScreenRoot, ScreenRoot>() {

					@Override
					public IGwtCompositeScreenRoot create(ScreenRoot screenRoot) {
						return new EditGroupScreen(screenRoot);
					}
				});
	}

	@Override
	protected void doCancel() {
		Actions.get().showWithParams2("directory", null);
	}

	@Override
	public void doLoad(ScreenRoot screenRoot) {
		screenRoot.getState().put("groupId", screenRoot.getState().get("entryUid"));
		screenRoot.getState().put("mailboxUid", screenRoot.getState().get("groupId"));
		super.doLoad(screenRoot);
	}

	public static ScreenElement screenModel() {
		GroupMenusConstants c = GWT.create(GroupMenusConstants.class);

		ScreenRoot screenRoot = ScreenRoot.create("editGroup", TYPE).cast();

		screenRoot.getHandlers().push(ModelHandler.create(null, GroupModelHandler.TYPE).readOnly()
				.withRole(BasicRoles.ROLE_MANAGE_GROUP).<ModelHandler>cast());
		screenRoot.getHandlers().push(ModelHandler.create(null, GroupMembersModelHandler.TYPE).readOnly()
				.withRole(BasicRoles.ROLE_MANAGE_GROUP_MEMBERS).<ModelHandler>cast());
		screenRoot.getHandlers().push(ModelHandler.create(null, MailboxGroupSharingModelHandler.TYPE)
				.withRole(BasicRoles.ROLE_MANAGE_GROUP_SHARINGS).<ModelHandler>cast());
		screenRoot.getHandlers().push(ModelHandler.create(null, DomainLoader.TYPE).<ModelHandler>cast());
		screenRoot.getHandlers().push(ModelHandler.create(null, GroupRolesModelHandler.TYPE)
				.withRole(BasicRoles.ROLE_MANAGE_GROUP).<ModelHandler>cast());
		screenRoot.getHandlers().push(ModelHandler.create(null, OrgUnitsAdministratorModelHandler.TYPE)
				.withRole(BasicRoles.ROLE_MANAGE_GROUP).<ModelHandler>cast());

		JsArray<Tab> tabs = JavaScriptObject.createArray().cast();
		tabs.push(Tab.create(null, c.generalTab(),
				ScreenElement.create(null, EditGroup.TYPE).readOnly().withRole(BasicRoles.ROLE_MANAGE_GROUP)));
		tabs.push(Tab.create(null, c.membersTab(), ScreenElement.create(null, EditGroupMembers.TYPE).readOnly()
				.withRole(BasicRoles.ROLE_MANAGE_GROUP_MEMBERS)));

		// mails
		JsArray<ScreenElement> mailsContent = JsArray.createArray().cast();
		mailsContent.push(ScreenElement.create(null, MailboxGroupEditor.TYPE).withRole(BasicRoles.ROLE_MANAGE_GROUP));
		mailsContent.push(ScreenElement.create(null, MailboxGroupSharingEditor.TYPE)
				.withRole(BasicRoles.ROLE_MANAGE_GROUP_SHARINGS));
		tabs.push(Tab.create(null, c.mailboxTab(), ContainerElement.create("editGroupGeneral", mailsContent)));

		// maintenance
		JsArray<ScreenElement> maintenanceContents = JsArray.createArray().cast();
		maintenanceContents
				.push(ScreenElement.create(null, ExternalIdEditor.TYPE).withRole(BasicRoles.ROLE_MANAGE_GROUP));
		maintenanceContents
				.push(ScreenElement.create(null, MailboxMaintenance.TYPE).withRole(BasicRoles.ROLE_MANAGE_GROUP));
		tabs.push(Tab.create(null, c.maintenanceTab(), ContainerElement.create(null, maintenanceContents)));

		// roles
		tabs.push(Tab.create(null, "Roles", ScreenElement.create(null, OrgUnitsAdministratorEditor.TYPE)
				.withRole(BasicRoles.ROLE_MANAGE_GROUP).witTitle("Roles")));
		TabContainer tab = TabContainer.create("editGroupTabs", tabs);
		screenRoot.setContent(tab);
		return screenRoot;
	}

}
