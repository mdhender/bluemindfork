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

import net.bluemind.gwtconsoleapp.base.editor.ScreenElementContributor;
import net.bluemind.gwtconsoleapp.base.menus.MenuContributor;
import net.bluemind.ui.gwtcalendar.client.GwtCalendarBundle;
import net.bluemind.ui.gwtuser.client.CalendarManagementModelHandler;
import net.bluemind.ui.gwtuser.client.CalendarsSubscriptionsEditor;
import net.bluemind.ui.gwtuser.client.FreebusySharingEditor;
import net.bluemind.ui.gwtuser.client.FreebusySharingModelHandler;
import net.bluemind.ui.gwtuser.client.UserCalendarsSharingModelHandler;
import net.bluemind.ui.gwtuser.client.UserCalendarsSharingsEditor;
import net.bluemind.ui.gwtuser.client.UserCalendarsSubscriptionModelHandler;
import net.bluemind.ui.gwtuser.client.UserSettingsCalendarsSharingModelHandler;

public class CalendarSettingsPlugin {

	public static void install() {

		MenuContributor.exportAsfunction("gwtSettingsCalendarMenusContributor",
				MenuContributor.create(new CalendarMenusContributor()));

		ScreenElementContributor.exportAsfunction("gwtSettingsCalendarScreensContributor",
				ScreenElementContributor.create(new CalendarScreensContributor()));

		UserCalendarsSharingsEditor.registerType();
		UserCalendarsSharingModelHandler.registerType();

		FreebusySharingEditor.registerType();
		FreebusySharingModelHandler.registerType();

		CalendarsSubscriptionsEditor.registerType();
		GeneralPartWidget.registerType();

		UserSettingsCalendarsSharingModelHandler.registerType();
		UserCalendarsSubscriptionModelHandler.registerType();
		CalendarManagementModelHandler.registerType();

		MyCalendarsPartWidget.registerType();
		GwtCalendarBundle.register();
	}
}
