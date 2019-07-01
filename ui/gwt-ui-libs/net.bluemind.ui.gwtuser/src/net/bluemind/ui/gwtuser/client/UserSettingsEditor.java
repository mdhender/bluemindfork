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
package net.bluemind.ui.gwtuser.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.i18n.client.TimeZone;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;
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
import net.bluemind.ui.common.client.forms.GwtTimeZone;
import net.bluemind.ui.gwtuser.client.l10n.UserSettingsConstants;

public class UserSettingsEditor extends CompositeGwtWidgetElement {

	public static final String TYPE = "bm.user.UserSettingsEditor";
	private final UserSettingsConstants messages = GWT.create(UserSettingsConstants.class);
	private FlexTable table;
	private ListBox lang;
	private ListBox tz;
	private ListBox dateFormat;
	private ListBox timeFormat;
	private ListBox defaultApp;
	private WidgetElement instance;

	public UserSettingsEditor(WidgetElement instance) {
		this.instance = instance;
		table = new FlexTable();
		table.setStyleName("formContainer");
		initWidget(table);

		int i = 0;

		lang = new ListBox();
		lang.addItem("Deutsch", "de");
		lang.addItem("English", "en");
		lang.addItem("Español", "es");
		lang.addItem("Français", "fr");
		lang.addItem("Italiano", "it");
		lang.addItem("Polski", "pl");
		lang.addItem("Slovenský", "sk");
		lang.addItem("中国的", "zh");

		ct(i++, messages.language(), lang);

		// Timezone
		// FIXME load tz ?
		tz = new ListBox();
		for (TimeZone t : GwtTimeZone.INSTANCE.getTimeZones()) {
			tz.addItem(t.getID(), t.getID());
		}

		table.setWidget(i, 0, new Label(messages.timezone()));
		ct(i++, messages.timezone(), tz);

		// Date format
		dateFormat = new ListBox();
		dateFormat.addItem("31/12/2012", "dd/MM/yyyy");
		dateFormat.addItem("2012-12-31", "yyyy-MM-dd");
		dateFormat.addItem("12/31/2012", "MM/dd/yyyy");
		ct(i++, messages.dateFormat(), dateFormat);

		// Time format
		timeFormat = new ListBox();
		timeFormat.addItem("1:00pm", "h:mma");
		timeFormat.addItem("13:00", "HH:mm");
		ct(i++, messages.timeFormat(), timeFormat);

		// FIXME ep for extend default app list ?
		// Default app

		defaultApp = new ListBox();
		defaultApp.addItem(messages.appCalendar(), "/cal/");
		defaultApp.addItem(messages.appMail(), "/webmail/");
		ct(i++, messages.defaultApp(), defaultApp);

		if (instance.isReadOnly()) {
			lang.setEnabled(false);
			tz.setEnabled(false);
			dateFormat.setEnabled(false);
			timeFormat.setEnabled(false);
			defaultApp.setEnabled(false);
		}
	}

	private void ct(int i, String label, Widget w) {
		table.setWidget(i, 0, new Label(label));
		table.setWidget(i, 1, w);
		table.getRowFormatter().setStyleName(i, "setting");
		table.getCellFormatter().setStyleName(i, 0, "label");
		table.getCellFormatter().setStyleName(i, 1, "form");

	}

	@Override
	public void saveModel(JavaScriptObject jsModel) {
		JsMapStringJsObject m = jsModel.cast();
		if (m.get("user-settings") == null) {
			m.put("user-settings", JavaScriptObject.createObject());
		}
		JSONObject model = new JSONObject(m.get("user-settings"));
		listboxGetValue(lang, model, "lang");
		listboxGetValue(tz, model, "timezone");
		listboxGetValue(dateFormat, model, "date");
		listboxGetValue(timeFormat, model, "timeformat");
		listboxGetValue(defaultApp, model, "default_app");
	}

	@Override
	public void loadModel(JavaScriptObject jsModel) {
		JsMapStringJsObject m = jsModel.cast();
		JSONObject model = new JSONObject(m.get("user-settings"));

		listboxSetValue(lang, value(model, "lang"));
		listboxSetValue(tz, value(model, "timezone"));
		listboxSetValue(dateFormat, value(model, "date"));
		listboxSetValue(timeFormat, value(model, "timeformat"));
		listboxSetValue(defaultApp, value(model, "default_app"));
	}

	private void listboxSetValue(ListBox listbox, String value) {
		if (value == null) {
			listbox.setSelectedIndex(-1);
		}
		for (int i = 0; i < listbox.getItemCount(); i++) {
			if (value.equals(listbox.getValue(i))) {
				listbox.setSelectedIndex(i);
				break;
			}
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

	private String value(JSONObject model, String key) {
		JSONValue value = model.get(key);
		if (value != null && value.isString() != null) {
			return value.isString().stringValue();
		} else {
			return null;
		}
	}

	public static void registerType() {
		GwtWidgetElement.register(TYPE, new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

			@Override
			public IGwtWidgetElement create(WidgetElement e) {
				return new UserSettingsEditor(e);
			}
		});
		GWT.log("bm.settings.UserMailIDentityEditor registred");

	}
}
