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

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HasEnabled;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.VerticalPanel;

public class YesNoPanel extends Composite implements HasEnabled {
	private final CalendarMessages messages = GWT.create(CalendarMessages.class);
	private RadioButton yesRadioButton;
	private RadioButton noRadioButton;
	private boolean enabled;

	public YesNoPanel(String groupName) {
		VerticalPanel panel = new VerticalPanel();
		yesRadioButton = new RadioButton(groupName, messages.yes());
		panel.add(yesRadioButton);

		noRadioButton = new RadioButton(groupName, messages.no());
		panel.add(noRadioButton);

		initWidget(panel);
	}

	public Boolean getValue() {
		if (noRadioButton.getValue() == Boolean.TRUE) {
			return Boolean.FALSE;
		} else if (yesRadioButton.getValue() == Boolean.TRUE) {
			return Boolean.TRUE;
		} else {
			return null;
		}
	}

	public void setValue(Boolean value) {
		if (value != null) {
			if (Boolean.TRUE.equals(value)) {
				yesRadioButton.setValue(true);
			} else {
				noRadioButton.setValue(true);
			}
		} else {
			yesRadioButton.setValue(Boolean.FALSE);
			noRadioButton.setValue(Boolean.TRUE);
		}
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
		yesRadioButton.setEnabled(enabled);
		noRadioButton.setEnabled(enabled);
	}
}
