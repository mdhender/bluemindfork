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
package net.bluemind.ui.adminconsole.directory.user;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;

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
import net.bluemind.role.api.BasicRoles;
import net.bluemind.ui.adminconsole.base.Actions;
import net.bluemind.ui.adminconsole.base.orgunit.OrgUnitsAdministratorEditor;
import net.bluemind.ui.adminconsole.base.orgunit.OrgUnitsAdministratorModelHandler;
import net.bluemind.ui.adminconsole.base.ui.BaseDirEntryEditScreen;
import net.bluemind.ui.adminconsole.directory.commons.ExternalIdEditor;
import net.bluemind.ui.adminconsole.directory.mailbox.MailboxMaintenance;
import net.bluemind.ui.adminconsole.directory.mailshare.DomainLoader;
import net.bluemind.ui.adminconsole.directory.user.l10n.UserConstants;
import net.bluemind.ui.adminconsole.directory.user.l10n.UserMenusConstants;
import net.bluemind.ui.gwtrole.client.UserRolesModelHandler;
import net.bluemind.ui.gwtuser.client.BooksSubscriptionsEditor;
import net.bluemind.ui.gwtuser.client.CalendarManagementModelHandler;
import net.bluemind.ui.gwtuser.client.CalendarsSubscriptionsEditor;
import net.bluemind.ui.gwtuser.client.FreebusySharingEditor;
import net.bluemind.ui.gwtuser.client.FreebusySharingModelHandler;
import net.bluemind.ui.gwtuser.client.UserBooksSharingModelHandler;
import net.bluemind.ui.gwtuser.client.UserBooksSharingsEditor;
import net.bluemind.ui.gwtuser.client.UserBooksSubscriptionModelHandler;
import net.bluemind.ui.gwtuser.client.UserCalendarsSharingModelHandler;
import net.bluemind.ui.gwtuser.client.UserCalendarsSharingsEditor;
import net.bluemind.ui.gwtuser.client.UserCalendarsSubscriptionModelHandler;
import net.bluemind.ui.gwtuser.client.UserSettingsModelHandler;
import net.bluemind.ui.gwtuser.client.UserTodolistsSharingModelHandler;
import net.bluemind.ui.gwtuser.client.UserTodolistsSharingsEditor;
import net.bluemind.ui.gwtuser.client.UserTodolistsSubscriptionModelHandler;
import net.bluemind.ui.gwtuser.client.UserTodolistsSubscriptionsEditor;
import net.bluemind.ui.gwtuser.client.l10n.FreeBusyConstants;
import net.bluemind.ui.mailbox.filter.MailSettingsModelHandler;
import net.bluemind.ui.mailbox.identity.UserMailIdentitiesModelHandler;
import net.bluemind.ui.settings.calendar.GeneralPartWidget;
import net.bluemind.user.api.gwt.js.JsUser;

public class EditUserScreen extends BaseDirEntryEditScreen {

	public static final String TYPE = "bm.ac.EditUserScreen";

	public interface BBBundle extends ClientBundle {

		@Source("EditUser.css")
		BBStyle getStyle();

	}

	public interface BBStyle extends CssResource {
		String archive();

		String typeColumn();

		String appLink();
	}

	private static final BBBundle bundle = GWT.create(BBBundle.class);

	private EditUserScreen(ScreenRoot screenRoot) {
		super(screenRoot);
		icon.setStyleName("fa fa-2x fa-user");

	}

	public void loadModel(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();
		JsUser user = map.get("user").cast();

		title.setInnerText(UserConstants.INST.editTitle(user.getLogin()));
		if (user.getArchived()) {
			title.setClassName(bundle.getStyle().archive());
		}
	}

	@Override
	public void doLoad(final ScreenRoot instance) {
		GWT.log("load model");
		instance.getState().put("userId", instance.getState().get("entryUid"));
		instance.getState().put("entryUid", instance.getState().get("entryUid"));
		instance.getState().put("mailboxUid", instance.getState().get("userId"));
		instance.getState().put("dirEntryUid", instance.getState().get("userId"));
		super.doLoad(instance);
	}

	public static void registerType() {
		GwtScreenRoot.registerComposite(TYPE, new IGwtDelegateFactory<IGwtCompositeScreenRoot, ScreenRoot>() {

			@Override
			public IGwtCompositeScreenRoot create(ScreenRoot screenRoot) {
				return new EditUserScreen(screenRoot);
			}
		});
	}

	@Override
	protected void doCancel() {
		Actions.get().showWithParams2("directory", null);
	}

	public static ScreenElement screenModel() {
		UserMenusConstants c = GWT.create(UserMenusConstants.class);
		ScreenRoot screenRoot = ScreenRoot.create("editUser", TYPE).cast();
		screenRoot.getHandlers().push(ModelHandler.create(null, EditUserModelHandler.TYPE).readOnly()
				.withRoles(BasicRoles.ROLE_MANAGE_USER, BasicRoles.ROLE_MANAGE_USER_VCARD).<ModelHandler>cast());
		screenRoot.getHandlers().push(ModelHandler.create(null, UserBooksSharingModelHandler.TYPE)
				.withRole(BasicRoles.ROLE_MANAGE_USER_SHARINGS).<ModelHandler>cast());
		screenRoot.getHandlers().push(ModelHandler.create(null, UserCalendarsSharingModelHandler.TYPE)
				.withRole(BasicRoles.ROLE_MANAGE_USER_SHARINGS).<ModelHandler>cast());
		screenRoot.getHandlers().push(ModelHandler.create(null, MailboxSharingModelHandler.TYPE)
				.withRole(BasicRoles.ROLE_MANAGE_USER_SHARINGS).<ModelHandler>cast());
		screenRoot.getHandlers().push(ModelHandler.create(null, UserSettingsModelHandler.TYPE).readOnly()
				.withRole(BasicRoles.ROLE_MANAGE_USER_SETTINGS).<ModelHandler>cast());
		screenRoot.getHandlers().push(ModelHandler.create(null, MailSettingsModelHandler.TYPE)
				.withRole(BasicRoles.ROLE_MANAGE_MAILBOX_FILTER).<ModelHandler>cast());
		screenRoot.getHandlers().push(ModelHandler.create(null, UserCalendarsSubscriptionModelHandler.TYPE)
				.withRole(BasicRoles.ROLE_MANAGE_USER_SUBSCRIPTIONS).<ModelHandler>cast());
		screenRoot.getHandlers().push(ModelHandler.create(null, UserBooksSubscriptionModelHandler.TYPE)
				.withRole(BasicRoles.ROLE_MANAGE_USER_SUBSCRIPTIONS).<ModelHandler>cast());

		screenRoot.getHandlers().push(ModelHandler.create(null, UserMailIdentitiesModelHandler.TYPE)
				.withRole(BasicRoles.ROLE_MANAGE_USER_MAIL_IDENTITIES).<ModelHandler>cast());
		screenRoot.getHandlers().push(ModelHandler.create(null, DomainLoader.TYPE).<ModelHandler>cast());
		screenRoot.getHandlers().push(ModelHandler.create(null, UserRolesModelHandler.TYPE)
				.withRole(BasicRoles.ROLE_MANAGE_USER).<ModelHandler>cast());
		screenRoot.getHandlers().push(ModelHandler.create(null, UserTodolistsSharingModelHandler.TYPE)
				.withRole(BasicRoles.ROLE_MANAGE_USER_SHARINGS).<ModelHandler>cast());
		screenRoot.getHandlers().push(ModelHandler.create(null, UserTodolistsSubscriptionModelHandler.TYPE)
				.withRole(BasicRoles.ROLE_MANAGE_USER_SUBSCRIPTIONS).<ModelHandler>cast());
		screenRoot.getHandlers().push(ModelHandler.create(null, FreebusySharingModelHandler.TYPE)
				.withRole(BasicRoles.ROLE_MANAGE_USER_SHARINGS).<ModelHandler>cast());
		screenRoot.getHandlers().push(ModelHandler.create(null, CalendarManagementModelHandler.TYPE)
				.withRole(BasicRoles.ROLE_MANAGE_USER).<ModelHandler>cast());

		screenRoot.getHandlers().push(ModelHandler.create(null, OrgUnitsAdministratorModelHandler.TYPE)
				.withRole(BasicRoles.ROLE_MANAGE_USER).<ModelHandler>cast());

		JsArray<Tab> tabs = JavaScriptObject.createArray().cast();
		// general
		JsArray<ScreenElement> editUserGeneralContents = JsArray.createArray().cast();
		editUserGeneralContents.push(
				ScreenElement.create(null, UserGeneralEditor.TYPE).readOnly().withRole(BasicRoles.ROLE_MANAGE_USER));
		editUserGeneralContents.push(ScreenElement.create(null, UserSettingsEditor.TYPE).readOnly()
				.withRole(BasicRoles.ROLE_MANAGE_USER_SETTINGS));
		editUserGeneralContents.push(ScreenElement.create(null, OrgUnitsAdministratorEditor.TYPE)
				.withRole(BasicRoles.ROLE_MANAGE_USER).witTitle(UserConstants.INST.roles()));

		ContainerElement editUserGeneral = ContainerElement.create("editUserGeneral", editUserGeneralContents);
		tabs.push(Tab.create(null, c.generalTab(), editUserGeneral));

		// vcard
		tabs.push(Tab.create(null, c.userVCardTab(), ScreenElement.create(null, "bluemind.contact.ui.ContactEditor")
				.withRole(BasicRoles.ROLE_MANAGE_USER_VCARD)));

		// mail
		ScreenElement mailEditor = UserMailEditor.model();
		tabs.push(Tab.create(null, c.mailSettingsTab(), mailEditor));

		// addressbooks
		JsArray<ScreenElement> addressbooksContent = JsArray.createArray().cast();
		addressbooksContent.push(ScreenElement.create(null, BooksSubscriptionsEditor.TYPE)
				.withRole(BasicRoles.ROLE_MANAGE_USER_SUBSCRIPTIONS));
		addressbooksContent.push(ScreenElement.create(null, UserBooksSharingsEditor.TYPE)
				.witTitle(c.addressBooksSharingsTab()).withRole(BasicRoles.ROLE_MANAGE_USER_SHARINGS));
		tabs.push(Tab.create(null, c.addressBooksTab(),
				ContainerElement.create("editUserAddressBook", addressbooksContent)));

		// calendars
		JsArray<ScreenElement> calendarsContent = JsArray.createArray().cast();
		calendarsContent.push(ScreenElement.create(null, GeneralPartWidget.TYPE).readOnly()
				.withRole(BasicRoles.ROLE_MANAGE_USER_SETTINGS));
		calendarsContent.push(ScreenElement.create(null, UserCalendarsEditor.TYPE).witTitle(c.userCalendars())
				.withRole(BasicRoles.ROLE_MANAGE_USER));

		calendarsContent.push(ScreenElement.create(null, UserCalendarsSharingsEditor.TYPE)
				.witTitle(c.calendarsSharingTab()).withRole(BasicRoles.ROLE_MANAGE_USER_SHARINGS));

		ScreenElement fbSharingEditor = ScreenElement.create(null, FreebusySharingEditor.TYPE)
				.withRole(BasicRoles.ROLE_MANAGE_USER_SHARINGS);
		fbSharingEditor.setTitle(FreeBusyConstants.INST.sharing());
		calendarsContent.push(fbSharingEditor);

		calendarsContent.push(ScreenElement.create(null, CalendarsSubscriptionsEditor.TYPE)
				.withRole(BasicRoles.ROLE_MANAGE_USER_SUBSCRIPTIONS));
		tabs.push(Tab.create(null, c.calendarSettingsTab(),
				ContainerElement.create("editUserCalendar", calendarsContent)));

		// todolists
		JsArray<ScreenElement> todolistsContet = JsArray.createArray().cast();
		todolistsContet.push(ScreenElement.create(null, UserTodolistsSharingsEditor.TYPE)
				.witTitle(c.todolistSharingsTab()).withRole(BasicRoles.ROLE_MANAGE_USER_SHARINGS));
		todolistsContet.push(ScreenElement.create(null, UserTodolistsSubscriptionsEditor.TYPE)
				.withRole(BasicRoles.ROLE_MANAGE_USER_SUBSCRIPTIONS));
		tabs.push(Tab.create(null, c.todolistSettingsTab(),
				ContainerElement.create("editUserTodolist", todolistsContet)));

		// maintenance
		JsArray<ScreenElement> maintenanceContents = JsArray.createArray().cast();
		UserPassword.registerType();
		Sudo.registerType();
		UserCheckAndRepair.registerType();

		maintenanceContents.push(
				ScreenElement.create(null, ExternalIdEditor.TYPE).withRole(BasicRoles.ROLE_MANAGE_USER_EXTERNAL_ID));
		maintenanceContents.push(
				ScreenElement.create(null, UserCheckAndRepair.TYPE).withRole(BasicRoles.ROLE_USER_CHECK_AND_REPAIR));
		maintenanceContents.push(ScreenElement.create(null, Sudo.TYPE).withRole(BasicRoles.ROLE_SUDO));
		maintenanceContents
				.push(ScreenElement.create(null, UserPassword.TYPE).withRole(BasicRoles.ROLE_MANAGE_USER_PASSWORD));

		maintenanceContents.push(
				ScreenElement.create(null, MailboxMaintenance.TYPE).withRole(BasicRoles.ROLE_USER_MAILBOX_MAINTENANCE));
		maintenanceContents
				.push(ScreenElement.create(null, DevicePanel.TYPE).withRole(BasicRoles.ROLE_MANAGE_USER_DEVICE));
		tabs.push(
				Tab.create(null, c.maintenanceTab(), ContainerElement.create("editMaintenance", maintenanceContents)));

		TabContainer tab = TabContainer.create("editUserTabs", tabs);
		screenRoot.setContent(tab);
		return screenRoot;
	}

}
