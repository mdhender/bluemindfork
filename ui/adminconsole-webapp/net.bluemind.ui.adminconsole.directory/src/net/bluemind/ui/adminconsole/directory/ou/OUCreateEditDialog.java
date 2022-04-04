/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.ui.adminconsole.directory.ou;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.utils.UIDGenerator;
import net.bluemind.directory.api.OrgUnit;
import net.bluemind.ui.adminconsole.directory.ou.l10n.OrgUnitConstants;
import net.bluemind.ui.adminconsole.directory.ou.model.OrgUnitItem;
import net.bluemind.ui.common.client.SizeHint;
import net.bluemind.ui.common.client.forms.DoneCancelActionBar;

public class OUCreateEditDialog extends Composite {

	interface OUEditDialogUiBinder extends UiBinder<DockLayoutPanel, OUCreateEditDialog> {

	}

	private static OUEditDialogUiBinder uiBinder = GWT.create(OUEditDialogUiBinder.class);

	private DockLayoutPanel dlp;

	@UiField
	DoneCancelActionBar actionBar;

	@UiField
	ScrollPanel content;

	@UiField
	Label modalTitle;

	private DialogBox os;
	private ScheduledCommand action;

	private ItemValue<OrgUnit> ouItem;

	public OUCreateEditDialog(OrgUnitItem item) {

		dlp = uiBinder.createAndBindUi(this);
		initWidget(dlp);

		modalTitle.setText(OrgUnitConstants.INST.qUpdate());

		OUEdit ie = new OUEdit(item);
		content.add(ie.asWidget());
		actionBar.setDoneAction(new ScheduledCommand() {

			@Override
			public void execute() {
				if (!ie.save()) {
					// error during save
					return;
				}
				if (action != null) {
					Scheduler.get().scheduleDeferred(action);
				}

				ouItem = ie.getOrgUnit();
				os.hide();
			}
		});
		cancelAction();
	}

	public OUCreateEditDialog(String domainUid) {

		dlp = uiBinder.createAndBindUi(this);
		initWidget(dlp);

		modalTitle.setText(OrgUnitConstants.INST.qCreate());

		OUCreate ie = new OUCreate(domainUid);
		content.add(ie.asWidget());
		actionBar.setDoneAction(new ScheduledCommand() {

			@Override
			public void execute() {
				if (!ie.save()) {
					// error during save
					return;
				}
				if (action != null) {
					Scheduler.get().scheduleDeferred(action);
				}

				ouItem = ItemValue.create(UIDGenerator.uid(), ie.getOrgUnit());
				os.hide();
			}
		});
		cancelAction();
	}

	/**
	 * @param scheduledCommand
	 */
	public void setAction(ScheduledCommand scheduledCommand) {
		action = scheduledCommand;
	}

	public SizeHint getSizeHint() {
		return new SizeHint(450, 300);

	}

	public void setOverlay(DialogBox os) {
		this.os = os;
	}

	private void cancelAction() {
		actionBar.setCancelAction(new ScheduledCommand() {

			@Override
			public void execute() {
				os.hide();
			}
		});
	}

	/**
	 * @return
	 */
	public ItemValue<OrgUnit> getOUItem() {
		return ouItem;
	}

}
