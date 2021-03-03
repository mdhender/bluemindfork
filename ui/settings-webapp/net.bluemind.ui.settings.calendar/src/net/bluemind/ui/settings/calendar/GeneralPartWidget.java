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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;

import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;
import net.bluemind.ui.common.client.forms.TimePicker;

// TODO move to gwtcalendar
public class GeneralPartWidget extends CompositeGwtWidgetElement {
	public static final String TYPE = "bm.settings.CalendarSettingsEditor";
	private FlexTable table;

	private final CalendarMessages messages = GWT.create(CalendarMessages.class);

	private ListBox weektartsOn;

	private ListBox defaultView;

	private YesNoPanel showWeekends;

	private TimePicker dayStartsAt;

	private TimePicker dayEndsAt;

	private CheckBox allDay;

	private ListBox workingDays;

	private YesNoPanel showDeclinedEvents;

	private DurationValue defaultEventAlert;

	private DurationValue defaultAlldayEventAlert;

	private ListBox defaultAlertMode;

	public GeneralPartWidget(WidgetElement instance) {
		table = new FlexTable();
		table.setStyleName("formContainer");
		initWidget(table);
		int i = 0;

		// First day of week
		weektartsOn = new ListBox();
		weektartsOn.addItem(messages.monday(), "monday");
		weektartsOn.addItem(messages.sunday(), "sunday");
		ct(i++, messages.weekStartsOn(), weektartsOn);

		// Default view
		defaultView = new ListBox();
		defaultView.addItem(messages.viewDay(), "day");
		defaultView.addItem(messages.viewWeek(), "week");
		defaultView.addItem(messages.viewMonth(), "month");
		defaultView.addItem(messages.viewList(), "agenda");
		ct(i++, messages.defaultView(), defaultView);

		// Show weekends
		showWeekends = new YesNoPanel("showWeekends");
		ct(i++, messages.showWeekends(), showWeekends);

		// Work Hours start
		dayStartsAt = new TimePicker();
		ct(i++, messages.dayStartsAt(), dayStartsAt);

		// Work Hours end
		dayEndsAt = new TimePicker();
		ct(i++, messages.dayEndsAt(), dayEndsAt);

		allDay = new CheckBox();
		ct(i++, messages.allDay(), allDay);

		allDay.addValueChangeHandler((h) -> {
			if (allDay.getValue()) {
				dayStartsAt.setSelectedIndex(0);
				dayEndsAt.setSelectedIndex(0);
				dayStartsAt.setEnabled(false);
				dayEndsAt.setEnabled(false);
			} else {
				dayStartsAt.setEnabled(true);
				dayEndsAt.setEnabled(true);
			}
		});

		// Working days
		workingDays = new ListBox();
		workingDays.setMultipleSelect(true);
		workingDays.addItem(messages.monday(), "mon");
		workingDays.addItem(messages.tuesday(), "tue");
		workingDays.addItem(messages.wednesday(), "wed");
		workingDays.addItem(messages.thusday(), "thu");
		workingDays.addItem(messages.friday(), "fri");
		workingDays.addItem(messages.saturday(), "sat");
		workingDays.addItem(messages.sunday(), "sun");
		workingDays.setVisibleItemCount(7);
		ct(i++, messages.workingDays(), workingDays);

		// Show declined events
		showDeclinedEvents = new YesNoPanel("showDeclinedEvents");
		ct(i++, messages.showDeclinedEvents(), showDeclinedEvents);

		// default alert
		defaultEventAlert = new DurationValue();
		ct(i++, messages.defaultEventAlert(), defaultEventAlert);

		defaultAlldayEventAlert = new DurationValue();
		ct(i++, messages.defaultAlldayEventAlert(), defaultAlldayEventAlert);

		defaultAlertMode = new ListBox();
		defaultAlertMode.setMultipleSelect(false);
		defaultAlertMode.addItem(messages.notification(), "Display");
		defaultAlertMode.addItem(messages.email(), "Email");
		ct(i++, messages.defaultAlertMode(), defaultAlertMode);

		if (instance.isReadOnly()) {
			workingDays.setEnabled(false);
			dayStartsAt.setEnabled(false);
			dayEndsAt.setEnabled(false);

			weektartsOn.setEnabled(false);
			defaultView.setEnabled(false);
			showWeekends.setEnabled(false);
			showDeclinedEvents.setEnabled(false);
			defaultEventAlert.setEnabled(false);
			defaultAlldayEventAlert.setEnabled(false);
			defaultAlertMode.setEnabled(false);
		}
	}

	@Override
	public void saveModel(JavaScriptObject m) {
		JsMapStringJsObject jsModel = m.cast();
		if (jsModel.get("user-settings") == null) {
			jsModel.put("user-settings", JavaScriptObject.createObject());
		}
		JSONObject model = new JSONObject(jsModel.get("user-settings"));
		listboxGetValue(weektartsOn, model, "day_weekstart");
		listboxGetValue(defaultView, model, "defaultview");
		model.put("showweekends", new JSONString("" + showWeekends.getValue()));
		listboxGetValue(dayStartsAt, model, "work_hours_start");
		listboxGetValue(dayEndsAt, model, "work_hours_end");
		listboxGetValues(workingDays, model, "working_days");
		model.put("show_declined_events", new JSONString("" + showDeclinedEvents.getValue()));
		Integer defaultEventAlertValue = defaultEventAlert.getValue();
		if (defaultEventAlertValue != null) {
			model.put("default_event_alert", new JSONString("" + Math.min(Integer.MAX_VALUE, defaultEventAlertValue)));
		} else {
			model.put("default_event_alert", new JSONString(""));
		}
		Integer defaultAlldayEventAlertValue = defaultAlldayEventAlert.getValue();
		if (defaultAlldayEventAlertValue != null) {
			model.put("default_allday_event_alert",
					new JSONString("" + Math.min(Integer.MAX_VALUE, defaultAlldayEventAlertValue)));
		} else {
			model.put("default_allday_event_alert", new JSONString(""));
		}
		model.put("default_event_alert_mode", new JSONString(defaultAlertMode.getSelectedValue()));
	}

	@Override
	public void loadModel(JavaScriptObject m) {
		JsMapStringJsObject jsModel = m.cast();
		JSONObject model = new JSONObject(jsModel.get("user-settings"));
		listboxSetValue(weektartsOn, value(model, "day_weekstart"));
		listboxSetValue(defaultView, value(model, "defaultview"));
		yesNoSetValue(showWeekends, value(model, "showweekends"));
		String whStart = value(model, "work_hours_start");
		listboxSetValue(dayStartsAt, whStart);
		String whEnd = value(model, "work_hours_end");
		listboxSetValue(dayEndsAt, whEnd);
		listboxSetValues(workingDays, value(model, "working_days"));
		yesNoSetValue(showDeclinedEvents, value(model, "show_declined_events"));

		setDuration(defaultEventAlert, value(model, "default_event_alert"));
		setDuration(defaultAlldayEventAlert, value(model, "default_allday_event_alert"));

		String value = value(model, "default_event_alert_mode");
		listboxSetValue(defaultAlertMode, value);

		if (defaultAlertMode.getSelectedIndex() < 0) {
			defaultAlertMode.setSelectedIndex(0);
		}

		validateAllDay(whStart, whEnd);
	}

	private void setDuration(DurationValue dv, String value) {
		if (value != null) {
			dv.setValue(Integer.parseInt(value));
		}

	}

	private void validateAllDay(String whStart, String whEnd) {
		if (whStart.equals("0") && whEnd.equals("0")) {
			dayStartsAt.setEnabled(false);
			dayEndsAt.setEnabled(false);
			allDay.setValue(true);
		} else {
			allDay.setValue(false);
		}
	}

	private void yesNoSetValue(YesNoPanel yn, String value) {
		if (value != null) {
			yn.setValue(Boolean.parseBoolean(value));
		} else {
			yn.setValue(null);
		}

	}

	private void ct(int i, String label, Widget w) {
		table.setWidget(i, 0, new Label(label));
		table.setWidget(i, 1, w);
		table.getRowFormatter().setStyleName(i, "setting");
		table.getCellFormatter().setStyleName(i, 0, "label");
		table.getCellFormatter().setStyleName(i, 1, "form");

	}

	private void listboxSetValue(ListBox listbox, String value) {
		if (value == null) {
			listbox.setSelectedIndex(-1);
			return;
		}
		for (int i = 0; i < listbox.getItemCount(); i++) {
			if (value.equals(listbox.getValue(i))) {
				listbox.setSelectedIndex(i);
				break;
			}
		}

	}

	private void listboxSetValues(ListBox listbox, String value) {
		if (value == null) {
			listbox.setSelectedIndex(-1);
			return;
		}

		HashSet<String> set = new HashSet<>(Arrays.asList(value.split(",")));
		for (int i = 0; i < listbox.getItemCount(); i++) {
			listbox.setItemSelected(i, set.contains(listbox.getValue(i)));
		}

	}

	private void listboxGetValue(ListBox listbox, JSONObject model, String key) {
		int index = listbox.getSelectedIndex();
		if (index >= 0) {
			model.put(key, new JSONString(listbox.getValue(index)));
		} else {
			model.put(key, null);
		}
	}

	private void listboxGetValues(ListBox box, JSONObject model, String key) {
		List<String> values = new ArrayList<>();
		for (int i = 0; i < box.getItemCount(); i++) {
			if (box.isItemSelected(i)) {
				values.add(box.getValue(i));
			}
		}

		String value = "";
		for (int i = 0; i < values.size(); i++) {
			value += values.get(i);
			if (i < values.size()) {
				value += ",";
			}
		}

		model.put(key, new JSONString(value));
	}

	private String value(JSONObject model, String key) {
		JSONValue value = model.get(key);

		if (value != null && value.isString() != null && value.isString().stringValue().length() > 0) {
			return value.isString().stringValue();
		} else {
			return null;
		}
	}

	public static void registerType() {
		GwtWidgetElement.register(TYPE, new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

			@Override
			public IGwtWidgetElement create(WidgetElement e) {
				return new GeneralPartWidget(e);
			}
		});
		GWT.log("bm.settings.CalendarSettingsEditor registred");

	}
}
