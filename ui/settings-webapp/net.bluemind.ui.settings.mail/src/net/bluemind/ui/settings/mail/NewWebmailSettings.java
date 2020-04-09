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
package net.bluemind.ui.settings.mail;

import java.util.logging.Logger;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.editor.client.Editor;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RadioButton;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.core.commons.gwt.JsMapStringString;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.ui.common.client.forms.Ajax;
import net.bluemind.ui.gwtuser.client.UserSettingsModelHandler;
import net.bluemind.user.api.gwt.endpoint.UserSettingsGwtEndpoint;

public class NewWebmailSettings extends CompositeGwtWidgetElement {

	public static String TYPE = "bm.mail.NewWebmailSettings";

	private static Logger logger = Logger.getLogger("NewWebmailSettings");
	
	private static final MailMessages messages = GWT.create(MailMessages.class);

	public class NewWebmailSettingsEdit extends Composite {
		RadioButton listStyleNormal = new RadioButton("listStyle", "Normal");
	    RadioButton listStyleFull = new RadioButton("listStyle", "Full");
	    RadioButton listStyleCompact = new RadioButton("listStyle", "Compact");
		
		public NewWebmailSettingsEdit() {
			FlexTable container = new FlexTable();
			
			Image normal = new Image(NewWebmailListStyleResources.INST.normal());
			Image full = new Image(NewWebmailListStyleResources.INST.full());
			Image compact = new Image(NewWebmailListStyleResources.INST.compact());
			
			container.setWidget(0, 0, listStyleFull);
			container.setWidget(1, 0, full);
			container.setWidget(0, 1, listStyleNormal);
			container.setWidget(1, 1, normal);
			container.setWidget(0, 2, listStyleCompact);
			container.setWidget(1, 2, compact);
			
			initWidget(container);
		}
		
	}
	
	private NewWebmailSettingsEdit editor;
	
	public NewWebmailSettings() {
		FlowPanel panel = new FlowPanel();
		Label title = new Label(messages.listStyle());
		title.setStyleName("sectionTitle");
		panel.setStyleName("newWebmail");
		panel.add(title);
		editor = new NewWebmailSettingsEdit();
		panel.add(editor);

		initWidget(panel);
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		
		final JsMapStringJsObject map = model.cast();
		JsMapStringString settingsValues = map.get("user-settings").cast();
		logger.info("yo yo");
		logger.info(settingsValues.get("mail_message_list_style"));

		String pref = settingsValues.get("mail_message_list_style");
		if (pref != null && isValid(pref)) {
			if (pref.equals("compact")) {
				editor.listStyleCompact.setValue(true);
			} else {
				editor.listStyleFull.setValue(true);
			}
		} else {
			editor.listStyleNormal.setValue(true);			
		}
	}
	
	private boolean isValid(String messageListPref) {
		return messageListPref.equals("compact") || messageListPref.equals("full");
	}

	@Override
	public void saveModel(JavaScriptObject model) {
		final JsMapStringJsObject map = model.cast();
		JsMapStringString settingsValues = map.get("user-settings").cast();
		
		Boolean isCompact = editor.listStyleCompact.getValue();
		Boolean isFull = editor.listStyleFull.getValue();
		if (Boolean.TRUE.equals(isCompact)) {
			settingsValues.put("mail_message_list_style", "compact");
		} else if (Boolean.TRUE.equals(isFull)) {
			settingsValues.put("mail_message_list_style", "full");
		} else {
			settingsValues.remove("mail_message_list_style");			
		}
	}

	public static void registerType() {
		GwtWidgetElement.register(TYPE, we -> new NewWebmailSettings());
	}

}
