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
package net.bluemind.ui.adminconsole.directory;

import net.bluemind.gwtconsoleapp.base.editor.ScreenElementContributor;
import net.bluemind.gwtconsoleapp.base.menus.MenuContributor;
import net.bluemind.ui.adminconsole.directory.addressbook.AddressBookModelHandler;
import net.bluemind.ui.adminconsole.directory.addressbook.AddressBookSharingEditor;
import net.bluemind.ui.adminconsole.directory.addressbook.AddressBookSharingModelHandler;
import net.bluemind.ui.adminconsole.directory.addressbook.EditAddressBook;
import net.bluemind.ui.adminconsole.directory.addressbook.EditAddressBookScreen;
import net.bluemind.ui.adminconsole.directory.addressbook.NewAddressBook;
import net.bluemind.ui.adminconsole.directory.addressbook.QCreateAddressBookModelHandler;
import net.bluemind.ui.adminconsole.directory.addressbook.QCreateAddressBookScreen;
import net.bluemind.ui.adminconsole.directory.calendar.CalendarModelHandler;
import net.bluemind.ui.adminconsole.directory.calendar.CalendarSettingsEditor;
import net.bluemind.ui.adminconsole.directory.calendar.CalendarSettingsModelHandler;
import net.bluemind.ui.adminconsole.directory.calendar.CalendarSharingEditor;
import net.bluemind.ui.adminconsole.directory.calendar.CalendarSharingModelHandler;
import net.bluemind.ui.adminconsole.directory.calendar.EditCalendar;
import net.bluemind.ui.adminconsole.directory.calendar.EditCalendarScreen;
import net.bluemind.ui.adminconsole.directory.calendar.NewCalendar;
import net.bluemind.ui.adminconsole.directory.calendar.QCreateCalendarModelHandler;
import net.bluemind.ui.adminconsole.directory.calendar.QCreateCalendarScreen;
import net.bluemind.ui.adminconsole.directory.commons.ExternalIdEditor;
import net.bluemind.ui.adminconsole.directory.externaluser.EditExternalUser;
import net.bluemind.ui.adminconsole.directory.externaluser.EditExternalUserScreen;
import net.bluemind.ui.adminconsole.directory.externaluser.ExternalUserModelHandler;
//import net.bluemind.ui.adminconsole.directory.externaluser.ExternalUserModelHandler;
import net.bluemind.ui.adminconsole.directory.externaluser.NewExternalUser;
import net.bluemind.ui.adminconsole.directory.externaluser.QCreateExternalUserModelHandler;
import net.bluemind.ui.adminconsole.directory.externaluser.QCreateExternalUserScreen;
import net.bluemind.ui.adminconsole.directory.group.EditGroup;
import net.bluemind.ui.adminconsole.directory.group.EditGroupMembers;
import net.bluemind.ui.adminconsole.directory.group.EditGroupScreen;
import net.bluemind.ui.adminconsole.directory.group.GroupMembersModelHandler;
import net.bluemind.ui.adminconsole.directory.group.GroupModelHandler;
import net.bluemind.ui.adminconsole.directory.group.MailboxGroupEditor;
import net.bluemind.ui.adminconsole.directory.group.MailboxGroupSharingEditor;
import net.bluemind.ui.adminconsole.directory.group.MailboxGroupSharingModelHandler;
import net.bluemind.ui.adminconsole.directory.group.NewGroup;
import net.bluemind.ui.adminconsole.directory.group.QCreateGroupModelHandler;
import net.bluemind.ui.adminconsole.directory.group.QCreateGroupScreen;
import net.bluemind.ui.adminconsole.directory.mailbox.MailboxMaintenance;
import net.bluemind.ui.adminconsole.directory.mailshare.DomainLoader;
import net.bluemind.ui.adminconsole.directory.mailshare.EditMailshareScreen;
import net.bluemind.ui.adminconsole.directory.mailshare.MailshareGeneralEditor;
import net.bluemind.ui.adminconsole.directory.mailshare.MailshareMailboxSharingEditor;
import net.bluemind.ui.adminconsole.directory.mailshare.MailshareMailboxSharingModelHandler;
import net.bluemind.ui.adminconsole.directory.mailshare.MailshareModelHandler;
import net.bluemind.ui.adminconsole.directory.mailshare.NewMailshare;
import net.bluemind.ui.adminconsole.directory.mailshare.QCreateMailshareModelHandler;
import net.bluemind.ui.adminconsole.directory.mailshare.QCreateMailshareScreen;
import net.bluemind.ui.adminconsole.directory.ou.OrgUnitsBrowser;
import net.bluemind.ui.adminconsole.directory.resource.EditResource;
import net.bluemind.ui.adminconsole.directory.resource.EditResourceScreen;
import net.bluemind.ui.adminconsole.directory.resource.NewResource;
import net.bluemind.ui.adminconsole.directory.resource.QCreateResourceModelHandler;
import net.bluemind.ui.adminconsole.directory.resource.QCreateResourceScreen;
import net.bluemind.ui.adminconsole.directory.resource.ResourceCalendarSharingEditor;
import net.bluemind.ui.adminconsole.directory.resource.ResourceCalendarSharingModelHandler;
import net.bluemind.ui.adminconsole.directory.resource.ResourceModelHandler;
import net.bluemind.ui.adminconsole.directory.resourcetype.EditResourceTypeScreen;
import net.bluemind.ui.adminconsole.directory.resourcetype.NewResourceType;
import net.bluemind.ui.adminconsole.directory.resourcetype.QCreateResourceTypeModelHandler;
import net.bluemind.ui.adminconsole.directory.resourcetype.QCreateResourceTypeScreen;
import net.bluemind.ui.adminconsole.directory.resourcetype.ResourceTypeCenter;
import net.bluemind.ui.adminconsole.directory.resourcetype.ResourceTypeGeneralEditor;
import net.bluemind.ui.adminconsole.directory.resourcetype.ResourceTypeModelHandler;
import net.bluemind.ui.adminconsole.directory.user.MailboxSharingModelHandler;
import net.bluemind.ui.adminconsole.directory.user.UserPackage;
import net.bluemind.ui.gwtaddressbook.client.GwtAddressbookBundle;
import net.bluemind.ui.gwtcalendar.client.GwtCalendarBundle;
import net.bluemind.ui.gwtrole.client.GroupRolesModelHandler;
import net.bluemind.ui.gwtsharing.client.AbstractDirEntryOpener;
import net.bluemind.ui.imageupload.client.ImageUpload;
import net.bluemind.ui.mailbox.filter.MailForwardEditor;
import net.bluemind.ui.mailbox.identity.IdentitiesModelHandler;
import net.bluemind.ui.mailbox.identity.IdentityManagement;
import net.bluemind.ui.mailbox.vacation.MailVacationEditor;

public class DirectoryPlugin {

	public static void init() {

		UserPackage.init();
		ImageUpload.exportFunction();
		MailForwardEditor.registerType();
		MailboxSharingModelHandler.registerType();

		// Group
		GroupRolesModelHandler.registerType();
		QCreateGroupScreen.registerType();
		QCreateGroupModelHandler.registerType();
		NewGroup.registerType();
		EditGroupScreen.registerType();
		GroupModelHandler.registerType();
		EditGroup.registerType();
		MailboxGroupEditor.registerType();
		EditGroupMembers.registerType();
		GroupMembersModelHandler.registerType();
		MailboxGroupSharingEditor.registerType();
		MailboxGroupSharingModelHandler.registerType();

		// commons
		ExternalIdEditor.registerType();

		// Mailshare
		QCreateMailshareScreen.registerType();
		QCreateMailshareModelHandler.registerType();
		NewMailshare.registerType();
		MailshareModelHandler.registerType();
		EditMailshareScreen.registerType();
		MailshareGeneralEditor.registerType();
		DomainLoader.registerType();
		MailVacationEditor.registerType();
		IdentityManagement.registerType();
		IdentitiesModelHandler.registerType();
		MailshareMailboxSharingEditor.registerType();
		MailshareMailboxSharingModelHandler.registerType();

		// Resource
		QCreateResourceModelHandler.registerType();
		QCreateResourceScreen.registerType();
		NewResource.registerType();
		EditResourceScreen.registerType();
		EditResource.registerType();
		ResourceModelHandler.registerType();
		ResourceCalendarSharingEditor.registerType();
		ResourceCalendarSharingModelHandler.registerType();

		// Resource type
		ResourceTypeCenter.registerType();
		QCreateResourceTypeModelHandler.registerType();
		QCreateResourceTypeScreen.registerType();
		NewResourceType.registerType();
		ResourceTypeGeneralEditor.registerType();
		ResourceTypeModelHandler.registerType();
		EditResourceTypeScreen.registerType();

		// AB
		QCreateAddressBookModelHandler.registerType();
		QCreateAddressBookScreen.registerType();
		NewAddressBook.registerType();
		EditAddressBook.registerType();
		EditAddressBookScreen.registerType();
		AddressBookModelHandler.registerType();
		AddressBookSharingEditor.registerType();
		AddressBookSharingModelHandler.registerType();
		GwtAddressbookBundle.register();

		// Calendar
		QCreateCalendarModelHandler.registerType();
		QCreateCalendarScreen.registerType();
		NewCalendar.registerType();
		EditCalendar.registerType();
		EditCalendarScreen.registerType();
		CalendarModelHandler.registerType();
		CalendarSharingEditor.registerType();
		CalendarSharingModelHandler.registerType();
		CalendarSettingsModelHandler.registerType();
		CalendarSettingsEditor.registerType();
		GwtCalendarBundle.register();

		// OU
		OrgUnitsBrowser.registerType();

		// External User
		QCreateExternalUserScreen.registerType();
		QCreateExternalUserModelHandler.registerType();
		NewExternalUser.registerType();
		EditExternalUserScreen.registerType();
		ExternalUserModelHandler.registerType();
		EditExternalUser.registerType();

		MenuContributor.exportAsfunction("NetBluemindUiAdminconsoleDirectoryContributor",
				MenuContributor.create(new DirectoryMenusContributor()));
		ScreenElementContributor.exportAsfunction("NetBluemindUiAdminconsoleDirectoryScreensContributor",
				ScreenElementContributor.create(new DirectoryScreensContributor()));

		MailboxMaintenance.registerType();

		AbstractDirEntryOpener.defaultOpener = new ACDirEntryOpener();
	}
}
