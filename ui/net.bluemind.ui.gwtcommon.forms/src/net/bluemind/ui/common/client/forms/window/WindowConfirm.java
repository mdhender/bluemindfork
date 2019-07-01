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
package net.bluemind.ui.common.client.forms.window;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

public class WindowConfirm extends DialogBox {
	private static final Binder binder = GWT.create(Binder.class);

	interface Binder extends UiBinder<Widget, WindowConfirm> {
	}

	@UiField
	DockLayoutPanel dlp;

	@UiField
	Label headerMsg;

	@UiField
	HTML contentMsg;

	@UiField
	Button ok;

	@UiField
	Button cancel;

	private Command okCmd;
	private Command cancelCmd;

	/**
	 * @param header
	 * @param content
	 * @param cmd
	 */
	public WindowConfirm() {
		setWidget(binder.createAndBindUi(this));
		setGlassEnabled(true);
		setGlassStyleName("bmOverlay");
		setModal(true);

		ok.getElement().setId("window-confirm-ok-button");
		ok.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				onOk();
			}
		});

		cancel.getElement().setId("window-confirm-cancel-button");
		cancel.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				onCancel();
			}
		});
	}

	@Override
	public void show() {
		super.show();
		super.center();
	}

	private void onOk() {
		if (okCmd != null) {
			okCmd.execute();
		}
		hide();
	}

	private void onCancel() {
		if (cancelCmd != null) {
			cancelCmd.execute();
		}
		hide();
	}

	@Override
	protected void onPreviewNativeEvent(NativePreviewEvent event) {
		super.onPreviewNativeEvent(event);
		switch (event.getTypeInt()) {
		case Event.ONKEYDOWN:
			if (event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ESCAPE) {
				onCancel();
				hide();
			} else if (event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ENTER) {
				onOk();
				hide();
			}
			break;
		}
	}

	public void setHeaderMsg(String msg) {
		headerMsg.setText(msg);
	}

	/**
	 * @param msg
	 *            the HTML message you want to have in the middle of the Window.
	 *            Support HTML contents. Warn : window is only 150px height !
	 */
	public void setContentMsg(String msg) {
		contentMsg.setHTML(msg);
	}

	public Command getOkCmd() {
		return okCmd;
	}

	public void setOkCmd(Command okCmd) {
		this.okCmd = okCmd;
	}

	public Command getCancelCmd() {
		return cancelCmd;
	}

	public void setCancelCmd(Command cancelCmd) {
		this.cancelCmd = cancelCmd;
	}

	public void setHeight(String height) {
		this.dlp.setHeight(height);
	}
}
