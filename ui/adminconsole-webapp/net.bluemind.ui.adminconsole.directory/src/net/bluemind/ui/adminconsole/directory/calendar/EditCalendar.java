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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;

import net.bluemind.calendar.api.gwt.js.JsCalendarDescriptor;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;
import net.bluemind.ui.adminconsole.base.ui.DelegationEdit;
import net.bluemind.ui.common.client.forms.StringEdit;
import net.bluemind.ui.gwtcalendar.client.bytype.CalendarTypeExtension;

public class EditCalendar extends CompositeGwtWidgetElement {

	interface EditCalUiBinder extends UiBinder<HTMLPanel, EditCalendar> {
	}

	public static final String TYPE = "bm.ac.EditCalendar";

	private EditCalUiBinder binder = GWT.create(EditCalUiBinder.class);

	@UiField
	StringEdit name;

	@UiField
	Label actionsHolder;

	@UiField
	DelegationEdit delegation;

	private WidgetElement widgetModel;

	public EditCalendar(WidgetElement widgetModel) {
		this.widgetModel = widgetModel;
		HTMLPanel dlp = binder.createAndBindUi(this);
		dlp.setHeight("100%");
		initWidget(dlp);

		name.setId("edit-calendar-name");

		name.setReadOnly(widgetModel.isReadOnly());
	}

	@Override
	public void saveModel(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();

		JsCalendarDescriptor cal = map.get("calendar").cast();
		cal.setName(name.asEditor().getValue());
		cal.setOrgUnitUid(delegation.asEditor().getValue());
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();
		JsCalendarDescriptor cal = map.get("calendar").cast();
		name.asEditor().setValue(cal.getName());
		String domainUid = map.getString("domainUid");
		delegation.setDomain(domainUid);
		delegation.asEditor().setValue(cal.getOrgUnitUid());

		if (!widgetModel.isReadOnly()) {
			String type = cal.getSettings().get("type");
			if (type == null) {
				type = "internal";
			}
			CalendarTypeExtension ext = CalendarTypeExtension.getExtensionByType(type);
			if (ext != null) {
				WidgetElement widget = ext.actionsWidget(map.getString("calendarId"), cal.getSettings());
				widget.attach(actionsHolder.getElement());
			}
		}
	}

	public static void registerType() {
		GwtWidgetElement.register(TYPE, new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

			@Override
			public IGwtWidgetElement create(WidgetElement e) {
				return new EditCalendar(e);
			}
		});
	}
}
