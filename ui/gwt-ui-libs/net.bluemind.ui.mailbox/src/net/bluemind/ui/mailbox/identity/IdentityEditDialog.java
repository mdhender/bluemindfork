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
package net.bluemind.ui.mailbox.identity;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.ScrollPanel;

import net.bluemind.mailbox.identity.api.IdentityDescription;
import net.bluemind.mailbox.identity.api.SignatureFormat;
import net.bluemind.ui.common.client.SizeHint;
import net.bluemind.ui.common.client.forms.DoneCancelActionBar;
import net.bluemind.user.api.UserMailIdentity;

public class IdentityEditDialog extends Composite {

	interface IdentityEditDialogUiBinder extends UiBinder<DockLayoutPanel, IdentityEditDialog> {

	}

	private static IdentityEditDialogUiBinder uiBinder = GWT.create(IdentityEditDialogUiBinder.class);

	private DockLayoutPanel dlp;

	@UiField
	DoneCancelActionBar actionBar;

	@UiField
	ScrollPanel content;

	private DialogBox os;
	private IdentityEdit ie;
	private ScheduledCommand action;
	private UserMailIdentity identity;

	/**
	 * @param templates
	 * @param entityId
	 */
	public IdentityEditDialog(List<IdentityDescription> templates, String mailboxUid,
			boolean supportsExternalIdentities) {
		this(newIdentity(mailboxUid, templates), templates, mailboxUid, supportsExternalIdentities);
	}

	/**
	 * @param ownerId
	 * @param templates
	 * @return
	 */
	private static UserMailIdentity newIdentity(String mailboxUid, List<IdentityDescription> templates) {
		UserMailIdentity identity = new UserMailIdentity();
		identity.mailboxUid = mailboxUid;
		// FIXME
		// identity.setOwner(ownerId);
		identity.format = SignatureFormat.HTML;
		identity.signature = "-- <br /><br />";
		for (IdentityDescription id : templates) {
			if (id.mbox != null && id.mbox.equals(mailboxUid)) {
				identity.email = id.email;
				identity.name = id.name;
				identity.signature = id.signature;
				if (id.isDefault != null && id.isDefault) {
					break;
				}
			}

		}
		return identity;
	}

	public IdentityEditDialog(UserMailIdentity userIdentity, final List<IdentityDescription> templates, String mboxUid,
			boolean supportsExternalIdentities) {

		dlp = uiBinder.createAndBindUi(this);
		initWidget(dlp);

		// FIXME where is the sent editr? !
		if (userIdentity.sentFolder == null) {
			userIdentity.sentFolder = "Sent";
		}
		ie = new IdentityEdit(userIdentity, templates, supportsExternalIdentities);
		this.identity = userIdentity;
		content.add(ie.asWidget());
		actionBar.setDoneAction(new ScheduledCommand() {

			@Override
			public void execute() {
				if (!ie.save()) {
					// error during save
					return;
				}
				identity.mailboxUid = null;
				for (IdentityDescription idd : templates) {
					if (idd.email.equals(identity.email)) {
						identity.mailboxUid = idd.mbox;
						break;
					}
				}
				if (action != null) {
					Scheduler.get().scheduleDeferred(action);
				}
				os.hide();
			}
		});

		actionBar.setCancelAction(new ScheduledCommand() {

			@Override
			public void execute() {
				cancel();
			}
		});

	}

	/**
	 * @param scheduledCommand
	 */
	public void setAction(ScheduledCommand scheduledCommand) {
		action = scheduledCommand;
	}

	public SizeHint getSizeHint() {
		return new SizeHint(575, 540);

	}

	public void setOverlay(DialogBox os) {
		this.os = os;
	}

	private void cancel() {
		os.hide();
	}

	/**
	 * @return
	 */
	public UserMailIdentity getIdentity() {
		return identity;
	}

}
