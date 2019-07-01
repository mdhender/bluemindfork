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
package net.bluemind.ui.settings.client;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;

public class NotificationPanel extends FlowPanel {
	final int VISIBLE_DELAY = 3000;
	private Timer delay;

	private Label lbl;

	public NotificationPanel() {
		super();
		lbl = new Label();
		lbl.setStyleName("msg");
		add(lbl);
		setStyleName("notification");
		setVisible(false);

		delay = new Timer() {
			public void run() {
				setVisible(false);
				lbl.setStyleName("msg");
			}
		};

	}

	public void showOk(String msg) {
		lbl.setText(msg);
		lbl.setStyleName("msg");
		lbl.addStyleName("ok");
		setVisible(true);
		delay.schedule(VISIBLE_DELAY);
	}

	public void showProgress(String msg) {
		lbl.setText(msg);
		lbl.addStyleName("progress");
		setVisible(true);
	}

	public void showError(String msg) {
		lbl.setText(msg);
		lbl.setStyleName("msg");
		lbl.addStyleName("error");
		setVisible(true);
		delay.schedule(VISIBLE_DELAY);
	}
}
