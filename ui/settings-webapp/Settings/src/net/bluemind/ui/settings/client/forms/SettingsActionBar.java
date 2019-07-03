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
package net.bluemind.ui.settings.client.forms;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.i18n.client.Constants;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;

public class SettingsActionBar extends ButtonBar {

	interface SettingsActionBarConstants extends Constants {
		String save();

		String cancel();

		String back();
	}

	private static final SettingsActionBarConstants constants = GWT.create(SettingsActionBarConstants.class);
	private FlowPanel panel;
	private Button saveBtn;

	public SettingsActionBar() {

		panel = new FlowPanel();

		Anchor back = new Anchor(constants.back());
		back.setHref(Document.get().getReferrer());
		back.setStyleName("back");
		panel.add(back);

		saveBtn = primaryButton();
		saveBtn.setText(constants.save());

		panel.add(saveBtn);

		initWidget(panel);
	}

	public Button getSaveBtn() {
		return saveBtn;
	}

	public void setSaveBtn(Button save) {
		this.saveBtn = save;
	}
}
