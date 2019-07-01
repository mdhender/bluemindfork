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
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;

public class DoneCancelActionBar extends ButtonBar {

	private static final CrudConstants cc = GWT.create(CrudConstants.class);

	private HorizontalPanel hp;
	private Button doneButton;
	private Button cancelButton;

	public DoneCancelActionBar() {
		super();
		this.hp = new HorizontalPanel();
		initWidget(hp);
		hp.addStyleName(style.hPanel());

		doneButton = newPrimaryButton("");
		doneButton.getElement().setId("done-cancel-action-bar-done");
		doneButton.setVisible(false);
		hp.add(doneButton);

		cancelButton = newStdButton(cc.cancel());
		cancelButton.getElement().setId("done-cancel-action-bar-cancel");
		cancelButton.setVisible(false);
		hp.add(cancelButton);

	}

	public void setCancelAction(final ScheduledCommand cancel) {
		cancelButton.setVisible(true);
		cancelButton.addClickHandler(event -> {
			Scheduler.get().scheduleDeferred(cancel);
		});
	}

	public Button setDoneAction(final ScheduledCommand done) {
		return setDoneAction(cc.done(), done);
	}

	public Button setDoneAction(final String label, final ScheduledCommand done) {
		doneButton.setText(label);
		doneButton.setVisible(true);
		doneButton.addClickHandler(event -> {
			Scheduler.get().scheduleDeferred(done);
		});
		return doneButton;
	}
}
