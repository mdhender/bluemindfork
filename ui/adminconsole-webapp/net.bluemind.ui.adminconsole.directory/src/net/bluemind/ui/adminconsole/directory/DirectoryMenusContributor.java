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

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.shared.GWT;

import net.bluemind.gwtconsoleapp.base.menus.Contributed;
import net.bluemind.gwtconsoleapp.base.menus.ContributorUtil;
import net.bluemind.gwtconsoleapp.base.menus.MenuContribution;
import net.bluemind.gwtconsoleapp.base.menus.MenuContributorUnwrapped;
import net.bluemind.gwtconsoleapp.base.menus.Screen;
import net.bluemind.gwtconsoleapp.base.menus.Section;
import net.bluemind.ui.adminconsole.directory.addressbook.AddressBookMenus;
import net.bluemind.ui.adminconsole.directory.calendar.CalendarMenus;
import net.bluemind.ui.adminconsole.directory.externaluser.ExternalUserMenus;
import net.bluemind.ui.adminconsole.directory.group.GroupMenus;
import net.bluemind.ui.adminconsole.directory.l10n.DirectoryMenusI18n;
import net.bluemind.ui.adminconsole.directory.mailshare.MailshareMenus;
import net.bluemind.ui.adminconsole.directory.ou.OUScreens;
import net.bluemind.ui.adminconsole.directory.resource.ResourceMenus;
import net.bluemind.ui.adminconsole.directory.resourcetype.ResourceTypeMenus;
import net.bluemind.ui.adminconsole.directory.user.UserMenus;

public class DirectoryMenusContributor implements MenuContributorUnwrapped {

	@Override
	public MenuContribution contribution() {

		DirectoryMenusI18n v = GWT.create(DirectoryMenusI18n.class);
		JsArray<Contributed<Section>> sections = JsArray.createArray().cast();

		JsArray<Screen> sscreens = JsArray.createArray().cast();
		sscreens.push(Screen.create("mailSettings", "Mail", null, false));
		sscreens.push(Screen.create("calendarSettings", "Calendar", null, false));
		sscreens.push(Screen.create("contactsSettings", "Contact", null, false));

		JsArray<Section> ssections = JsArray.createArray().cast();
		ssections.push(Section.createVerySimple("dir1", v.quickCreates(), null));
		JsArray<Screen> dir2Screens = JsArray.createArray().cast();
		dir2Screens.push(Screen.create("directory", v.directoryBrowser(), null, true));
		ssections.push(Section.createSimple("dir2", v.entitiesManagement(), null, dir2Screens));

		sections.push(Contributed.create(null,
				Section.create("directories", v.directories(), 99, "fa-th-list", sscreens, ssections)));

		JsArray<Contributed<Screen>> screens = JsArray.createArray().cast();
		MenuContribution contrib = MenuContribution.create(sections, screens);

		return ContributorUtil.mergeMenuContributions(contrib,
				UserMenus.getContribution(),
				GroupMenus.getContribution(), MailshareMenus.getContribution(),
				ResourceMenus.getContribution(),
				ResourceTypeMenus.getContribution(),
				AddressBookMenus.getContribution(),
				CalendarMenus.getContribution(),
				ExternalUserMenus.getContribution(),
				OUScreens.getMenuContribution());
	}

}