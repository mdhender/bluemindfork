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
package net.bluemind.ui.adminconsole.dataprotect;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RadioButton;

import net.bluemind.dataprotect.api.RestoreOperation;
import net.bluemind.ui.adminconsole.dataprotect.l10n.DPTexts;
import net.bluemind.ui.adminconsole.dataprotect.l10n.DpTextsHelper;

public class DPRestoreDialog extends DialogBox {
	private ScheduledCommand okCommand;
	private FlexTable content;

	public DPRestoreDialog() {
		FlowPanel buttons = new FlowPanel();
		Button ok = new Button(DPTexts.INST.restore());
		ok.addStyleName("button");
		ok.addStyleName("primary");
		ok.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				if (okCommand != null) {
					hide();
					Scheduler.get().scheduleDeferred(okCommand);
				}
			}
		});
		Button cancel = new Button(DPTexts.INST.cancel());
		cancel.addStyleName("button");
		cancel.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				hide();
			}
		});

		buttons.add(ok);
		buttons.add(cancel);
		buttons.getElement().getStyle().setPadding(5, Unit.PX);
		buttons.setStyleName("modal-dialog-buttons");

		content = new FlexTable();
		DockLayoutPanel dlp = new DockLayoutPanel(Unit.PX);
		Label l = new Label(DPTexts.INST.restoreDialogTitle());
		l.setStyleName("modal-dialog-title");
		dlp.addNorth(l, 30);
		dlp.addNorth(content, 300);
		dlp.addSouth(buttons, 30);

		dlp.setHeight("260px");
		dlp.setWidth("472px");
		setWidget(dlp);

		setStyleName("dialog");
		setGlassEnabled(true);
		setGlassStyleName("settingsOverlay");
		addStyleName("gwt-DialogBox");
		setModal(true);
		setAutoHideEnabled(false);
	}

	public void setOkCommand(ScheduledCommand sc) {
		this.okCommand = sc;
	}

	public void addRestorableOperation(RestoreOperation rop, final ScheduledCommand cmd) {
		RadioButton rb = new RadioButton("restoreOps");
		rb.setText(DpTextsHelper.translate(rop.identifier));
		content.setWidget(content.getRowCount(), 1, rb);
		rb.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				setOkCommand(cmd);
			}
		});
	}
}
