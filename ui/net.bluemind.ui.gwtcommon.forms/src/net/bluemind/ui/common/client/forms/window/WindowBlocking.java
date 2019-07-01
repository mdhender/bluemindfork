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
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

public class WindowBlocking extends DialogBox {
	private static final Binder binder = GWT.create(Binder.class);

	interface Binder extends UiBinder<Widget, WindowBlocking> {
	}

	@UiField
	DockLayoutPanel dlp;

	@UiField
	Label headerMsg;

	@UiField
	HTML contentMsg;

	/**
	 * This class is aimed at creating a DialogBox with glass enabled and
	 * without buttons. It is mainly used to display a blocking information to
	 * the user, waiting for the system to automatically refresh.
	 */
	public WindowBlocking() {
		setWidget(binder.createAndBindUi(this));
		setGlassEnabled(true);
		setGlassStyleName("bmOverlay");
		setModal(true);
	}

	@Override
	public void show() {
		super.show();
		super.center();
	}

	/**
	 * @param msg
	 *            the message you want to display in the header part of the
	 *            window
	 */
	public void setHeaderMsg(String msg) {
		headerMsg.setText(msg);
	}

	/**
	 * @param msg
	 *            the HTML message you want to have in the middle of the Window.
	 *            Support HTML contents.
	 */
	public void setContentMsg(String msg) {
		contentMsg.setHTML(msg);
	}

	/**
	 * @param height
	 *            the height of the window displayed. Default is 150px.
	 */
	public void setHeight(String height) {
		this.dlp.setHeight(height);
	}
}
