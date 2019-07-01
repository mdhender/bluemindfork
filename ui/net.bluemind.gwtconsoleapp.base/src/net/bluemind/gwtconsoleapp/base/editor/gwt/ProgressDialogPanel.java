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
package net.bluemind.gwtconsoleapp.base.editor.gwt;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

public class ProgressDialogPanel extends DialogBox {

	@UiField
	Label text;

	@UiField
	SimplePanel progress;

	private int percent;

	private int steps = 100;

	private static final Binder binder = GWT.create(Binder.class);

	interface Binder extends UiBinder<Widget, ProgressDialogPanel> {
	}

	public ProgressDialogPanel() {
		setWidget(binder.createAndBindUi(this));
		setGlassEnabled(true);
		setGlassStyleName("settingsOverlay");
		setModal(true);
	}

	public void setSteps(int steps) {
		this.steps = steps;
		this.percent = 0;
		setProgress(percent, "");
	}

	private void setProgress(int percent, String label) {
		progress.setWidth(percent + "%");
		text.setText(label);
		// GWT.log("percent " + percent);
		if (percent >= 100) {
			hide();
		}
	}

	public void progress(int increasePercent, String label) {
		percent += increasePercent;
		if (percent >= steps) {
			percent = steps;
		}
		setProgress((int) (((double) percent / (double) steps) * 100.0), label);
	}

	public void setText(String s) {
		text.setText(s);
	}

	@Override
	public void show() {
		super.show();
		this.center();
	}

}
