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
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;

import net.bluemind.calendar.api.gwt.js.JsCalendarDescriptor;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.gwt.js.JsItemValue;
import net.bluemind.core.container.model.gwt.serder.ItemValueGwtSerDer;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.gwt.js.JsDomain;
import net.bluemind.domain.api.gwt.serder.DomainGwtSerDer;
import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;
import net.bluemind.ui.adminconsole.base.ui.DelegationEdit;
import net.bluemind.ui.common.client.errors.ErrorCodeTexts;
import net.bluemind.ui.gwtcalendar.client.bytype.CalendarTypeExtension;
import net.bluemind.ui.gwtcalendar.client.bytype.CreateCalendarWidget;

public class NewCalendar extends CompositeGwtWidgetElement {

	public static final String TYPE = "bm.ac.QCreateCalendarWidget";

	private static CalUiBinder uiBinder = GWT.create(CalUiBinder.class);

	interface CalUiBinder extends UiBinder<HTMLPanel, NewCalendar> {

	}

	private HTMLPanel dlp;

	@UiField
	Label errorLabel;

	@UiField
	ListBox calendarType;

	@UiField(provided = false)
	CreateCalendarWidget typeSpecific = new CreateCalendarWidget();

	@UiField
	DelegationEdit delegation;

	private NewCalendar() {
		dlp = uiBinder.createAndBindUi(this);
		initWidget(dlp);
		dlp.setHeight("100%");

		for (CalendarTypeExtension t : typeSpecific.types()) {
			calendarType.addItem(t.getLabel(), t.getType());
		}

		calendarType.addChangeHandler(new ChangeHandler() {

			@Override
			public void onChange(ChangeEvent event) {
				changeCalendarType();
			}
		});

	}

	protected void changeCalendarType() {
		String value = calendarType.getSelectedValue();
		if (value == null) {
			value = "internal";
		}

		typeSpecific.show(value);
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();

		if (map.get("domain") != null) {
			JsItemValue<JsDomain> domain = map.get("domain").cast();

			ItemValue<Domain> d = new ItemValueGwtSerDer<>(new DomainGwtSerDer()).deserialize(new JSONObject(domain));
			if (d.value.global) {
				errorLabel.setText(ErrorCodeTexts.INST.getString("NOT_IN_GLOBAL_DOMAIN"));
			} else {
				errorLabel.setText("");
			}

			JsCalendarDescriptor cal = map.get("calendar").cast();
			String type = cal.getSettings().get("type");
			if (type == null) {
				type = "internal";
			}

			typeSpecific.show(type);
			if (d != null) {
				delegation.setDomain(d.uid);
			}
		}

	}

	@Override
	public void saveModel(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();

		JavaScriptObject t = map.get("calendar");
		JsCalendarDescriptor cal = t.cast();
		cal.setOrgUnitUid(delegation.asEditor().getValue());
		typeSpecific.saveModel(cal);
	}

	public static void registerType() {
		GwtWidgetElement.register(TYPE, new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

			@Override
			public IGwtWidgetElement create(WidgetElement e) {
				return new NewCalendar();
			}
		});
	}
}
