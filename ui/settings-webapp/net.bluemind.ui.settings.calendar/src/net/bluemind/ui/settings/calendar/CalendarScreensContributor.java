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
package net.bluemind.ui.settings.calendar;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;

import net.bluemind.gwtconsoleapp.base.editor.ContainerElement;
import net.bluemind.gwtconsoleapp.base.editor.ScreenElement;
import net.bluemind.gwtconsoleapp.base.editor.ScreenElementContribution;
import net.bluemind.gwtconsoleapp.base.editor.ScreenElementContributorUnwrapper;
import net.bluemind.gwtconsoleapp.base.editor.Tab;
import net.bluemind.gwtconsoleapp.base.editor.TabContainer;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.ui.gwtuser.client.CalendarManagementModelHandler;
import net.bluemind.ui.gwtuser.client.CalendarsSubscriptionsEditor;
import net.bluemind.ui.gwtuser.client.FreebusySharingEditor;
import net.bluemind.ui.gwtuser.client.FreebusySharingModelHandler;
import net.bluemind.ui.gwtuser.client.UserCalendarsSharingsEditor;
import net.bluemind.ui.gwtuser.client.UserCalendarsSubscriptionModelHandler;
import net.bluemind.ui.gwtuser.client.UserSettingsCalendarsSharingModelHandler;
import net.bluemind.ui.gwtuser.client.l10n.FreeBusyConstants;

public class CalendarScreensContributor implements ScreenElementContributorUnwrapper {

	private static final CalendarMessages messages = GWT.create(CalendarMessages.class);

	@Override
	public JsArray<ScreenElementContribution> contribution() {

		JsArray<ScreenElement> userGeneralElts = JsArray.createArray().cast();
		userGeneralElts.push(ScreenElement.create(null, GeneralPartWidget.TYPE).readOnly()

				.withRole(BasicRoles.ROLE_SELF_CHANGE_SETTINGS));
		ContainerElement calendarGeneralContainer = ContainerElement.create("calendarGeneralContainer",
				userGeneralElts);

		JsArray<Tab> tabs = JsArray.createArray().cast();

		tabs.push(Tab.create(null, messages.tabGeneral(), calendarGeneralContainer));

		JsArray<ScreenElement> myCalendarsElements = JsArray.createArray().cast();
		myCalendarsElements.push(ScreenElement.create(null, "bm.ContainerElement"));

		tabs.push(Tab.create(null, messages.tabFolders(), ContainerElement.create("myCalendars", myCalendarsElements)));

		tabs.push(Tab.create(null, messages.tabSubscriptions(),
				ScreenElement.create(null, CalendarsSubscriptionsEditor.TYPE)));

		tabs.push(
				Tab.create(null, messages.tabSharings(), ScreenElement.create(null, UserCalendarsSharingsEditor.TYPE)));

		ScreenElement contribution = TabContainer.create("/cal/", tabs);

		JsArray<ScreenElementContribution> contribs = JsArray.createArray().cast();

		contribs.push(ScreenElementContribution.create("root", "childrens", contribution));

		contribs.push(ScreenElementContribution.create("myCalendars", "childrens",
				ScreenElement.create(null, MyCalendarsPartWidget.TYPE)));

		ScreenElement fbSharingEditor = ScreenElement.create(null, FreebusySharingEditor.TYPE);
		fbSharingEditor.setTitle(FreeBusyConstants.INST.sharing());
		contribs.push(ScreenElementContribution.create("myCalendars", "childrens", fbSharingEditor));

		contribs.push(ScreenElementContribution.create("base", "modelHandlers",
				ScreenElement.create(null, UserSettingsCalendarsSharingModelHandler.TYPE)));

		contribs.push(ScreenElementContribution.create("base", "modelHandlers",
				ScreenElement.create(null, UserCalendarsSubscriptionModelHandler.TYPE)));

		contribs.push(ScreenElementContribution.create("base", "modelHandlers",
				ScreenElement.create(null, CalendarManagementModelHandler.TYPE)));
		contribs.push(ScreenElementContribution.create("base", "modelHandlers",
				ScreenElement.create(null, FreebusySharingModelHandler.TYPE)));

		return contribs;
	}

}
