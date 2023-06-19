/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.ui.adminconsole.system.certificate.smime;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;

import net.bluemind.smime.cacerts.api.SmimeCacertInfos;
import net.bluemind.smime.cacerts.api.SmimeRevocation;
import net.bluemind.ui.common.client.forms.DoneCancelActionBar;

public class SmimeRevocationsDialog extends Composite {

	interface SmimeRevocationsDialogDialogUiBinder extends UiBinder<DockLayoutPanel, SmimeRevocationsDialog> {
	}

	@UiField
	Label issuer;

	@UiField
	Label subject;

	@UiField
	DoneCancelActionBar actionBar;

	@UiField
	SimplePanel revocationsListPanel;

	@UiField
	SmimeRevocationsGrid revocationsGrid;

	@UiField
	Label emptyRevocationLabel;

	private static SmimeRevocationsDialogDialogUiBinder binder = GWT.create(SmimeRevocationsDialogDialogUiBinder.class);

	private DialogBox os;
	private DockLayoutPanel dlp;

	public SmimeRevocationsDialog(SmimeCacertInfos infos) {
		dlp = binder.createAndBindUi(this);
		initWidget(dlp);

		issuer.setText(infos.cacertIssuer);
		subject.setText(infos.cacertSubject);
		loadRevokedList(infos.revocations);

		actionBar.setDoneAction(new ScheduledCommand() {
			@Override
			public void execute() {
				os.hide();
			}
		});

		actionBar.setCancelAction(new ScheduledCommand() {
			@Override
			public void execute() {
				os.hide();
			}
		});
	}

	void openDialog() {
		os = new DialogBox();
		os.addStyleName("dialog");
		setSize("1000px", "300px");
		os.setWidget(this);
		os.setGlassEnabled(true);
		os.setAutoHideEnabled(false);
		os.setGlassStyleName("modalOverlay");
		os.setModal(false);
		os.center();
		os.setAutoHideEnabled(true);
		os.show();
	}

	private void loadRevokedList(List<SmimeRevocation> revocations) {
		revocationsGrid.setValues(revocations);
		revocationsListPanel.setVisible(!revocations.isEmpty());
		revocationsGrid.setVisible(!revocations.isEmpty());
		emptyRevocationLabel.setVisible(revocations.isEmpty());
	}

}
