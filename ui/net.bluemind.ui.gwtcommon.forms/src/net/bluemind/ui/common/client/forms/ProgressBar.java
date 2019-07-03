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
package net.bluemind.ui.common.client.forms;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;

public class ProgressBar extends Composite {

	interface ProgressBarUiBinder extends UiBinder<HTMLPanel, ProgressBar> {

	}

	private ProgressBarUiBinder uib = GWT.create(ProgressBarUiBinder.class);

	private HTMLPanel panel;

	@UiField
	SimplePanel progress;

	@UiField
	Label progressText;

	private int percent;

	public ProgressBar() {
		this.panel = uib.createAndBindUi(this);
		initWidget(panel);
		restart();
	}

	public void update(String string) {
		progressText.setText(string);
	}

	public void setProgressPercent(int pct) {
		this.percent = pct;
		progress.setWidth(Math.min(100, percent) + "%");
	}

	public void incr() {
		setProgressPercent(percent + 1);
	}

	public void restart() {
		setProgressPercent(0);
		progress.addStyleName("bar-success");
		progress.removeStyleName("bar-danger");
	}

	public void fail(String string) {
		progress.removeStyleName("bar-success");
		progress.addStyleName("bar-danger");
		update(string);
	}

}
