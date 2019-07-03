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
package net.bluemind.ui.settings.addressbook.management;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteEvent;
import com.google.gwt.user.client.ui.Hidden;

import net.bluemind.ui.common.client.OverlayScreen;
import net.bluemind.ui.common.client.SizeHint;
import net.bluemind.ui.common.client.forms.DoneCancelActionBar;

public class VCFUploadDialog extends Composite {

	interface VCFUploadDialogUiBinder extends UiBinder<DockLayoutPanel, VCFUploadDialog> {

	}

	private static VCFUploadDialogUiBinder uiBinder = GWT.create(VCFUploadDialogUiBinder.class);

	private DockLayoutPanel dlp;

	@UiField
	DoneCancelActionBar actionBar;

	@UiField
	FormPanel formPanel;

	@UiField
	FileUpload vcf;

	@UiField
	Hidden bookId;

	private OverlayScreen os;

	public VCFUploadDialog(String containerUid) {
		dlp = uiBinder.createAndBindUi(this);
		dlp.getElement().setAttribute("style", "z-index:1000");
		initWidget(dlp);
		bookId.setValue(containerUid);

		actionBar.setDoneAction(new ScheduledCommand() {

			@Override
			public void execute() {
				formPanel.submit();
			}
		});

		actionBar.setCancelAction(new ScheduledCommand() {

			@Override
			public void execute() {
				cancel();
			}
		});

		formPanel.setEncoding(FormPanel.ENCODING_MULTIPART);
		formPanel.setMethod(FormPanel.METHOD_POST);
		formPanel.setAction("ab/import?addressbook=" + containerUid);
		formPanel.addSubmitCompleteHandler(new FormPanel.SubmitCompleteHandler() {
			public void onSubmitComplete(SubmitCompleteEvent event) {
				os.hide();
			}
		});

	}

	public SizeHint getSizeHint() {
		return new SizeHint(400, 200);

	}

	public void setOverlay(OverlayScreen os) {
		this.os = os;
	}

	private void cancel() {
		os.hide();
	}
}
