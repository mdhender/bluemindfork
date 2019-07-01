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
package net.bluemind.ui.adminconsole.directory.calendar;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.shared.GWT;

import net.bluemind.calendar.api.gwt.js.JsCalendarDescriptor;
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
import net.bluemind.ui.adminconsole.base.ui.BaseEditScreen;
import net.bluemind.ui.adminconsole.directory.calendar.l10n.CalendarMenusConstants;

public class EditCalendarScreen extends BaseEditScreen {

	private static final String TYPE = "bm.ac.EditCalendarScreen";

	private EditCalendarScreen(ScreenRoot screenRoot) {
		super(screenRoot);
		icon.setStyleName("fa fa-2x fa-calendar");
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();
		JsCalendarDescriptor cal = map.get("calendar").cast();
		title.setInnerText(cal.getName());
	}

	public static void registerType() {
		GwtScreenRoot.registerComposite(TYPE, new IGwtDelegateFactory<IGwtCompositeScreenRoot, ScreenRoot>() {

			@Override
			public IGwtCompositeScreenRoot create(ScreenRoot screenRoot) {
				return new EditCalendarScreen(screenRoot);
			}
		});
	}

	@Override
	public void doLoad(ScreenRoot screenRoot) {
		screenRoot.getState().put("calendarId", screenRoot.getState().get("entryUid"));
		super.doLoad(screenRoot);
	}

	@Override
	protected void doCancel() {
		Actions.get().showWithParams2("directory", null);
	}

	public static ScreenElement screenModel() {
		//
		CalendarMenusConstants c = GWT.create(CalendarMenusConstants.class);
		ScreenRoot screenRoot = ScreenRoot.create("editCalendar", TYPE).cast();
		screenRoot.getHandlers().push(ModelHandler.create(null, CalendarModelHandler.TYPE).readOnly()
				.withRole(BasicRoles.ROLE_MANAGE_DOMAIN_CAL).<ModelHandler> cast());
		screenRoot.getHandlers().push(ModelHandler.create(null, CalendarSharingModelHandler.TYPE)
				.withRole(BasicRoles.ROLE_MANAGE_DOMAIN_CAL_SHARING).<ModelHandler> cast());
		screenRoot.getHandlers().push(ModelHandler.create(null, CalendarSettingsModelHandler.TYPE)
				.withRole(BasicRoles.ROLE_MANAGE_DOMAIN_CAL).<ModelHandler> cast());

		JsArray<Tab> tabs = JavaScriptObject.createArray().cast();
		// general
		JsArray<ScreenElement> main = JsArray.createArray().cast();
		main.push(ScreenElement.create(null, EditCalendar.TYPE).withRole(BasicRoles.ROLE_MANAGE_DOMAIN_CAL).readOnly());
		main.push(ScreenElement.create(null, CalendarSettingsEditor.TYPE).withRole(BasicRoles.ROLE_MANAGE_DOMAIN_CAL));
		tabs.push(Tab.create(null, c.generalTab(), ContainerElement.create("editCalendarMainTab", main)));
		tabs.push(Tab.create(null, c.sharingTab(), ScreenElement.create(null, CalendarSharingEditor.TYPE)
				.withRole(BasicRoles.ROLE_MANAGE_DOMAIN_CAL_SHARING)));

		TabContainer tab = TabContainer.create("editCalendarTabs", tabs);
		screenRoot.setContent(tab);
		return screenRoot;
	}

}
