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
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.BorderStyle;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;

public class DPConfirm extends DialogBox {

	private ScheduledCommand okCommand;

	public DPConfirm(String label) {
		FlowPanel buttons = new FlowPanel();
		Button ok = new Button("OK");
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

		Button cancel = new Button("Cancel");
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

		DockLayoutPanel dlp = new DockLayoutPanel(Unit.PX);
		dlp.addSouth(buttons, 40);

		Label lbl = new Label(label);
		lbl.getElement().getStyle().setPadding(5, Unit.PX);
		dlp.add(lbl);
		dlp.setHeight("100px");
		dlp.setWidth("300px");
		Style style = dlp.getElement().getStyle();
		style.setZIndex(1000);
		style.setBackgroundColor("white");
		style.setBorderColor("#9db0bf");
		style.setBorderStyle(BorderStyle.SOLID);
		style.setBorderWidth(1, Unit.PX);
		setWidget(dlp);

		setGlassEnabled(true);
		setGlassStyleName("settingsOverlay");
		setModal(true);
		setAutoHideEnabled(false);
	}

	public void setOkCommand(ScheduledCommand sc) {
		this.okCommand = sc;
	}
}
