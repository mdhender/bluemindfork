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

import net.bluemind.ui.adminconsole.base.orgunit.OrgUnitsAdministratorEditor;
import net.bluemind.ui.adminconsole.base.orgunit.OrgUnitsAdministratorModelHandler;
import net.bluemind.ui.adminconsole.directory.DirectoryCenter;
import net.bluemind.ui.gwtrole.client.RolesEditor;
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
import net.bluemind.ui.mailbox.filter.MailSettingsModelHandler;
import net.bluemind.ui.mailbox.filter.SieveEdit;
import net.bluemind.ui.mailbox.identity.UserIdentityManagement;
import net.bluemind.ui.mailbox.identity.UserMailIdentitiesModelHandler;

public class UserPackage {

	public static void init() {
		QCreateUserScreen.registerType();
		QCreateUserModelHandler.registerType();
		NewUser.registerType();

		UserBooksSharingModelHandler.registerType();
		UserBooksSharingsEditor.registerType();

		UserCalendarsSharingModelHandler.registerType();
		UserCalendarsSharingsEditor.registerType();

		UserGeneralEditor.registerType();
		UserMailEditor.registerType();
		EditUserScreen.registerType();
		EditUserModelHandler.registerType();
		UserSettingsEditor.registerType();
		UserSettingsModelHandler.registerType();
		DirectoryCenter.registerType();
		MailSettingsModelHandler.registerType();
		SieveEdit.registerType();

		UserMailIdentitiesModelHandler.registerType();
		UserIdentityManagement.registerType();
		net.bluemind.ui.settings.calendar.GeneralPartWidget.registerType();

		BooksSubscriptionsEditor.registerType();
		CalendarsSubscriptionsEditor.registerType();

		UserBooksSubscriptionModelHandler.registerType();
		UserCalendarsSubscriptionModelHandler.registerType();

		UserTodolistsSharingModelHandler.registerType();
		UserTodolistsSubscriptionModelHandler.registerType();
		UserTodolistsSharingsEditor.registerType();
		UserTodolistsSubscriptionsEditor.registerType();

		MailboxSharingEditor.registerType();

		DevicePanel.registerType();

		RolesEditor.registerType();
		UserRolesModelHandler.registerType();
		FreebusySharingEditor.registerType();
		FreebusySharingModelHandler.registerType();

		UserCalendarsEditor.registerType();
		CalendarManagementModelHandler.registerType();

		OrgUnitsAdministratorEditor.registerType();
		OrgUnitsAdministratorModelHandler.registerType();
	}

}
