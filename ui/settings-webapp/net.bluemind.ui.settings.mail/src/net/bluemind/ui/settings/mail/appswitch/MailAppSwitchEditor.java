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
package net.bluemind.ui.settings.mail.appswitch;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;

import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;
import net.bluemind.ui.common.client.forms.SwitchButton;

public class MailAppSwitchEditor extends CompositeGwtWidgetElement {

	private static final String MAIL_APPLICATION = "mail-application";
	private static final String USER_SETTINGS = "user-settings";
	private static final String WEBMAIL = "webmail";
	private static final String MAIL_WEBAPP = "mail-webapp";
	public static final String TYPE = "bm.mail.MailAppSwitchEditor";
	private SwitchButton appSwitch;

	public MailAppSwitchEditor() {
		appSwitch = new SwitchButton("appSwitchButton", false, MailAppSwitchConstants.INST.appSwitchOn(),
				MailAppSwitchConstants.INST.appSwitchOff());

		FlowPanel panel = new FlowPanel();
		Label title = new Label(MailAppSwitchConstants.INST.appSwitchTitle());
		title.setStyleName("sectionTitle");
		panel.add(title);

		FlexTable flexTable = new FlexTable();
		flexTable.setStyleName("formContainer");
		flexTable.setWidget(0, 0, new Label(MailAppSwitchConstants.INST.appSwitchLabel()));
		flexTable.setWidget(0, 1, appSwitch);
		flexTable.getRowFormatter().setStyleName(0, "setting");
		flexTable.getCellFormatter().setStyleName(0, 0, "label");
		flexTable.getCellFormatter().setStyleName(0, 1, "form");
		panel.add(flexTable);

		initWidget(panel);
	}

	@Override
	public void loadModel(JavaScriptObject m) {
		appSwitch.setValue(MAIL_WEBAPP.equals(readMailAppSetting(m)));
	}

	@Override
	public void saveModel(JavaScriptObject m) {
		String value = Boolean.TRUE.equals(appSwitch.getValue()) ? MAIL_WEBAPP : WEBMAIL;
		writeMailAppSetting(m, value);
	}

	private String readMailAppSetting(JavaScriptObject m) {
		JsMapStringJsObject map = m.cast();
		JSONObject model = new JSONObject(map.get(USER_SETTINGS));
		JSONValue value = model.get(MAIL_APPLICATION);
		if (value != null && value.isString() != null) {
			return value.isString().stringValue();
		}
		return null;
	}

	private void writeMailAppSetting(JavaScriptObject m, String value) {
		JsMapStringJsObject map = m.cast();
		if (map.get(USER_SETTINGS) == null) {
			map.put(USER_SETTINGS, JavaScriptObject.createObject());
		}
		JSONObject model = new JSONObject(map.get(USER_SETTINGS));
		model.put(MAIL_APPLICATION, new JSONString(value));
	}

	public static void registerType() {

		GwtWidgetElement.register(TYPE, new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

			@Override
			public IGwtWidgetElement create(WidgetElement e) {
				return new MailAppSwitchEditor();
			}
		});
	}
}
