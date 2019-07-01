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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

import net.bluemind.gwtconsoleapp.base.editor.ScreenElement;
import net.bluemind.gwtconsoleapp.base.editor.ScreenElementContribution;
import net.bluemind.gwtconsoleapp.base.editor.ScreenElementContributorUnwrapper;
import net.bluemind.ui.adminconsole.directory.addressbook.AddressBookScreens;
import net.bluemind.ui.adminconsole.directory.calendar.CalendarScreens;
import net.bluemind.ui.adminconsole.directory.externaluser.ExternalUserScreens;
import net.bluemind.ui.adminconsole.directory.group.GroupScreens;
import net.bluemind.ui.adminconsole.directory.mailshare.MailshareScreens;
import net.bluemind.ui.adminconsole.directory.ou.OUScreens;
import net.bluemind.ui.adminconsole.directory.resource.ResourceScreens;
import net.bluemind.ui.adminconsole.directory.resourcetype.ResourceTypeScreens;
import net.bluemind.ui.adminconsole.directory.user.UserScreens;

public class DirectoryScreensContributor implements ScreenElementContributorUnwrapper {

	@Override
	public JsArray<ScreenElementContribution> contribution() {
		JsArray<ScreenElementContribution> rootContribs = JsArray.createArray().cast();
		rootContribs.push(
				ScreenElementContribution.create(null, null, ScreenElement.create("directory", DirectoryCenter.TYPE)));

		JsArray<ScreenElementContribution> userContribs = UserScreens.getContributions();
		JsArray<ScreenElementContribution> groupContribs = GroupScreens.getContributions();
		JsArray<ScreenElementContribution> mailshareContribs = MailshareScreens.getContributions();
		JsArray<ScreenElementContribution> resourceContribs = ResourceScreens.getContributions();
		JsArray<ScreenElementContribution> resourceTypeContribs = ResourceTypeScreens.getContributions();
		JsArray<ScreenElementContribution> abContribs = AddressBookScreens.getContributions();
		JsArray<ScreenElementContribution> calContribs = CalendarScreens.getContributions();
		JsArray<ScreenElementContribution> ouContribs = OUScreens.getContributions();
		JsArray<ScreenElementContribution> externalUserContribs = ExternalUserScreens
				.getContributions();

		return join(userContribs, rootContribs, groupContribs, mailshareContribs, resourceContribs,
				resourceTypeContribs, abContribs, calContribs, ouContribs,
				externalUserContribs);
	}

	private <T extends JavaScriptObject> JsArray<T> join(JsArray<T>... arrays) {
		JsArray<T> ret = JsArray.createArray().cast();
		for (int i = 0; i < arrays.length; i++) {
			JsArray<T> a = arrays[i];
			for (int j = 0; j < a.length(); j++) {
				ret.push(a.get(j));
			}
		}
		return ret;
	}
}
