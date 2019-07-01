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
package net.bluemind.ui.im.client.conversation;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;

import net.bluemind.ui.im.client.IMConstants;

public class ConversationTab extends Composite {
	private static ConversationTabUiBinder uiBinder = GWT.create(ConversationTabUiBinder.class);

	interface ConversationTabUiBinder extends UiBinder<FlowPanel, ConversationTab> {
	}

	public interface ConversationTabBundle extends ClientBundle {
		@Source("ConversationTab.css")
		ConversationTabStyle getStyle();
	}

	public interface ConversationTabStyle extends CssResource {
		public String tab();

		public String closeBtn();

		public String displayName();

		public String closeBtnContainer();
	}

	public static ConversationTabStyle style;
	public static ConversationTabBundle bundle;
	private int unread;

	@UiField
	Label displayName;

	@UiField
	Label unreadCount;

	@UiField
	FlowPanel closeBtnContainer;

	@UiField
	Label closeBtn;

	/**
	 * @param conversationName
	 */
	public ConversationTab(String conversationName) {
		initWidget(uiBinder.createAndBindUi(this));
		unread = 1;

		bundle = GWT.create(ConversationTabBundle.class);
		style = bundle.getStyle();
		style.ensureInjected();

		setStyleName(style.tab());

		displayName.setText(conversationName);
		displayName.setStyleName(style.displayName());
		setDownlight();
		unreadCount.setStyleName("unreadCount");

		closeBtnContainer.setStyleName(style.closeBtnContainer());
		closeBtn.setStyleName("fa fa-lg fa-close");
		closeBtn.setTitle(IMConstants.INST.closeConversationButton());

	}

	public void setCloseBtnAction(ClickHandler ch) {
		closeBtn.addClickHandler(ch);
	}

	public void setHighlight() {
		unreadCount.setText(Integer.toString(unread));
		unreadCount.setVisible(true);
		unread++;
	}

	public int getUnread() {
		return unread;
	}

	public void setUnread(int unread) {
		this.unread = unread;
	}

	public void setDownlight() {
		unread = 1;
		unreadCount.setVisible(false);
	}

}
