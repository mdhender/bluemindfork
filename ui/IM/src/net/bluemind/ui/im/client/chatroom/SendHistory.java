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
package net.bluemind.ui.im.client.chatroom;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.Widget;

import net.bluemind.ui.im.client.IMCtrl;
import net.bluemind.ui.im.client.IScreen;

public class SendHistory extends DialogBox implements IScreen {

	private String roomName;

	private static final Binder binder = GWT.create(Binder.class);

	interface Binder extends UiBinder<Widget, SendHistory> {
	}

	@UiField
	Button send;

	@UiField
	Button cancel;

	@UiField
	FlowPanel form;

	private final RecipientSearchBox searchBox;

	public SendHistory() {
		setWidget(binder.createAndBindUi(this));
		setGlassEnabled(true);
		setGlassStyleName("bmOverlay");
		setModal(true);

		SuggestOracle oracle = new RecipientSearchBoxOracle();
		searchBox = new RecipientSearchBox(oracle);
		searchBox.setWidth("460px");

		form.add(searchBox);

		send.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				send();
			}
		});

		cancel.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				hide();
			}
		});

	}

	@Override
	public void show() {
		super.show();
		searchBox.setValue(null);
		searchBox.setFocus(true);
	}

	private void send() {
		if (!searchBox.getValue().isEmpty()) {
			IMCtrl.getInstance().sendHistory(roomName, searchBox.getValue());
			hide();
		}
	}

	@Override
	protected void onPreviewNativeEvent(NativePreviewEvent event) {
		super.onPreviewNativeEvent(event);
		switch (event.getTypeInt()) {
		case Event.ONKEYDOWN:
			if (event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ESCAPE) {
				hide();
			} else if (event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ENTER) {
				send();
			}
			break;
		}

	}

	public void setRoomName(String roomName) {
		this.roomName = roomName;
	}

}
