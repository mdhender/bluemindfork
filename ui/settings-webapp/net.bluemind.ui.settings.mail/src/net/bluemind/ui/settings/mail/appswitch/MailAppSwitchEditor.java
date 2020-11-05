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
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;

import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;
import net.bluemind.ui.common.client.forms.SwitchButton;
import net.bluemind.ui.mailbox.filter.MailSettingsModel;

public class MailAppSwitchEditor extends CompositeGwtWidgetElement {

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
		MailSettingsModel model = MailSettingsModel.get(m);
		if (MAIL_WEBAPP.equals(model.getMailApplication())) {
			appSwitch.setValue(true);
		} else {
			appSwitch.setValue(false);
		}
	}

	@Override
	public void saveModel(JavaScriptObject m) {
		MailSettingsModel model = MailSettingsModel.get(m);
		if (Boolean.TRUE.equals(appSwitch.getValue())) {
			model.setMailApplication(MAIL_WEBAPP);
		} else {
			model.setMailApplication(WEBMAIL);
		}
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
