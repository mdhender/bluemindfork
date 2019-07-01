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
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.i18n.client.TimeZone;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.ListBox;

import net.bluemind.calendar.api.CalendarSettingsData;
import net.bluemind.calendar.api.CalendarSettingsData.Day;
import net.bluemind.calendar.api.gwt.js.JsCalendarSettingsData;
import net.bluemind.calendar.api.gwt.js.JsCalendarSettingsDataDay;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;
import net.bluemind.ui.adminconsole.directory.calendar.l10n.CalendarSettingsConstants;
import net.bluemind.ui.common.client.forms.GwtTimeZone;
import net.bluemind.ui.common.client.forms.TimePickerMs;

public class CalendarSettingsEditor extends CompositeGwtWidgetElement {

	interface SettingsUiBinder extends UiBinder<HTMLPanel, CalendarSettingsEditor> {
	}

	public static final String TYPE = "bm.ac.CalendarSettingsEditor";

	private SettingsUiBinder binder = GWT.create(SettingsUiBinder.class);

	@UiField
	ListBox workingDays;

	@UiField
	TimePickerMs dayStartsAt;

	@UiField
	TimePickerMs dayEndsAt;

	@UiField
	CheckBox allDay;

	@UiField
	ListBox minDuration;

	@UiField
	ListBox tz;

	private WidgetElement instance;

	public CalendarSettingsEditor(WidgetElement instance) {
		this.instance = instance;
		HTMLPanel dlp = binder.createAndBindUi(this);
		dlp.setHeight("100%");
		initWidget(dlp);

		CalendarSettingsConstants c = GWT.create(CalendarSettingsConstants.class);
		workingDays.addItem(c.sunday(), CalendarSettingsData.Day.SU.name());
		workingDays.addItem(c.monday(), CalendarSettingsData.Day.MO.name());
		workingDays.addItem(c.tuesday(), CalendarSettingsData.Day.TU.name());
		workingDays.addItem(c.wednesday(), CalendarSettingsData.Day.WE.name());
		workingDays.addItem(c.thusday(), CalendarSettingsData.Day.TH.name());
		workingDays.addItem(c.friday(), CalendarSettingsData.Day.FR.name());
		workingDays.addItem(c.saturday(), CalendarSettingsData.Day.SA.name());

		minDuration.addItem(c.durationOneHour(), "60");
		minDuration.addItem(c.durationTwoHours(), "120");
		minDuration.addItem(c.durationHalfDay(), "720");
		minDuration.addItem(c.durationDay(), "1440");

		for (TimeZone t : GwtTimeZone.INSTANCE.getTimeZones()) {
			tz.addItem(t.getID(), t.getID());
		}

		if (instance.isReadOnly()) {
			workingDays.setEnabled(false);
			dayStartsAt.setEnabled(false);
			dayEndsAt.setEnabled(false);
			minDuration.setEnabled(false);
			tz.setEnabled(false);
		}

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

	}

	@Override
	public void saveModel(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();

		JsCalendarSettingsData cal = map.get("calendar-settings").cast();

		JsArray<JsCalendarSettingsDataDay> days = JsArrayString.createArray().cast();
		for (int i = 0; i < workingDays.getItemCount(); i++) {
			if (workingDays.isItemSelected(i)) {
				days.push(JsCalendarSettingsDataDay.create(Day.valueOf(workingDays.getValue(i))));
			}
		}
		cal.setWorkingDays(days);
		cal.setDayStart(Integer.parseInt(dayStartsAt.getSelectedValue()));
		cal.setDayEnd(Integer.parseInt(dayEndsAt.getSelectedValue()));
		cal.setMinDuration(Integer.parseInt(minDuration.getSelectedValue()));
		cal.setTimezoneId(tz.getSelectedValue());
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();
		JsCalendarSettingsData cal = map.get("calendar-settings").cast();

		if (cal.getWorkingDays() != null) {
			JsArray<JsCalendarSettingsDataDay> days = cal.getWorkingDays();
			for (int i = 0; i < days.length(); i++) {
				workingDays.setItemSelected(Day.valueOf(days.get(i).value()).ordinal(), true);
			}
		}

		if (null != cal.getTimezoneId()) {
			setListBoxValue(tz, cal.getTimezoneId());
		}

		Integer minDurationValue = cal.getMinDuration();
		if (minDurationValue != null) {
			String dur = String.valueOf(minDurationValue);
			setListBoxValue(minDuration, dur);
		}
		Integer dayStart = cal.getDayStart();
		dayStartsAt.setValue(dayStart);
		Integer dayEnd = cal.getDayEnd();
		dayEndsAt.setValue(dayEnd);

		validateAllDay(dayStart, dayEnd);
	}

	private void validateAllDay(int whStart, int whEnd) {
		if (whStart == 0 && whEnd == 0) {
			dayStartsAt.setEnabled(false);
			dayEndsAt.setEnabled(false);
			allDay.setValue(true);
		} else {
			allDay.setValue(false);
		}
	}

	private void setListBoxValue(ListBox l, String value) {
		for (int i = 0; i < l.getItemCount(); i++) {
			if (value.equals(l.getValue(i))) {
				l.setSelectedIndex(i);
				break;
			}
		}
	}

	public static void registerType() {
		GwtWidgetElement.register("bm.ac.CalendarSettingsEditor",
				new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

					@Override
					public IGwtWidgetElement create(WidgetElement e) {
						return new CalendarSettingsEditor(e);
					}
				});
	}
}
