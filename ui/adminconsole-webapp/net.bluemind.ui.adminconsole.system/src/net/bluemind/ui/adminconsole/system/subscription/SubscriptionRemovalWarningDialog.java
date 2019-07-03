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
package net.bluemind.ui.adminconsole.system.subscription;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.DockLayoutPanel;

import net.bluemind.ui.adminconsole.system.subscription.l10n.SubscriptionConstants;
import net.bluemind.ui.common.client.forms.DoneCancelActionBar;

public class SubscriptionRemovalWarningDialog extends Composite {

	interface SubscriptionRemovalWarningDialogUiBinder
			extends UiBinder<DockLayoutPanel, SubscriptionRemovalWarningDialog> {

	}

	@UiField
	DoneCancelActionBar actionBar;

	private static SubscriptionRemovalWarningDialogUiBinder binder = GWT
			.create(SubscriptionRemovalWarningDialogUiBinder.class);

	private DialogBox os;
	private DockLayoutPanel dlp;
	private boolean delete;

	private Runnable doDelete;

	public SubscriptionRemovalWarningDialog(Runnable doDelete) {
		this.doDelete = doDelete;
		dlp = binder.createAndBindUi(this);
		initWidget(dlp);

		delete = false;

		actionBar.setDoneAction(SubscriptionConstants.INST.removeSubscriptionAction(), new ScheduledCommand() {

			@Override
			public void execute() {
				delete = true;
				save();

			}
		});

		actionBar.setCancelAction(new ScheduledCommand() {

			@Override
			public void execute() {
				os.hide();
			}
		});

	}

	public void setOverlay(DialogBox os) {
		this.os = os;
	}

	public boolean isDelete() {
		return delete;
	}

	private void save() {
		os.hide();
		doDelete.run();
	}

}
