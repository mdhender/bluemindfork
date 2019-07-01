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

import java.util.Date;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SimpleHtmlSanitizer;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;

import net.bluemind.ui.im.client.MessageFormatter;
import net.bluemind.ui.im.client.Photo;

public class Message extends Composite {

	private static MessageUiBinder uiBinder = GWT.create(MessageUiBinder.class);

	interface MessageUiBinder extends UiBinder<HTMLPanel, Message> {
	}

	public interface MessageBundle extends ClientBundle {
		@Source("Message.css")
		MessageStyle getStyle();
	}

	public interface MessageStyle extends CssResource {
		public String container();

		public String date();

		public String msg();

		public String unread();

		public String bubble();

		public String avatar();

		public String from();

		public String arrowOutline();

		public String arrow();

		public String arrowOutlineUnread();

		public String arrowUnread();

		public String footer();

		public String downlight();

		public String arrowOutlineDownlight();

		public String arrowDownlight();
	}

	public static MessageStyle style;
	public static MessageBundle bundle;

	@UiField
	Photo photo;

	@UiField
	Label from;

	@UiField
	HTMLPanel msgContainer;

	@UiField
	Label date;

	@UiField
	FlowPanel bubble;

	@UiField
	HTMLPanel arrow;

	@UiField
	HTMLPanel arrowOutline;

	@UiField
	HTMLPanel footer;

	private Date timestamp;

	public Message() {
		initWidget(uiBinder.createAndBindUi(this));
		bundle = GWT.create(MessageBundle.class);
		style = bundle.getStyle();
		style.ensureInjected();

		setStyleName(style.container());

		arrow.setStyleName(style.arrow());
		arrowOutline.setStyleName(style.arrowOutline());

		bubble.setStyleName(style.bubble());
		photo.setSize(28);
		photo.setStyleName(style.avatar());
		footer.setStyleName(style.footer());
		from.setStyleName(style.from());
		date.setStyleName(style.date());

		timestamp = new Date();
	}

	public void setHeaderText(String headerText) {
		from.setText(headerText);
	}

	public void setDate(String dateText) {
		date.setText(dateText);
	}

	public void setPicture(String dataurl) {
		photo.set(dataurl, 28);
	}

	public void setColor(int index) {
		String css = "c" + index;
		photo.addStyleName(css);
		from.addStyleName(css);
		bubble.addStyleName(css);
		arrowOutline.addStyleName("c" + css);
	}

	public void setUnread() {
		bubble.addStyleName(style.unread());
		arrowOutline.addStyleName(style.arrowOutlineUnread());
		arrow.addStyleName(style.arrowUnread());
	}

	public void markAsRead() {
		bubble.removeStyleName(style.unread());
		arrowOutline.removeStyleName(style.arrowOutlineUnread());
		arrow.removeStyleName(style.arrowUnread());

	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	/**
	 * @param message
	 */
	public void appendMessage(String message) {
		SafeHtml sanitized = SimpleHtmlSanitizer.sanitizeHtml(message);
		HTMLPanel msg = new HTMLPanel(MessageFormatter.convert(sanitized.asString()));
		msg.setStyleName(style.msg());
		msgContainer.add(msg);
	}

	/**
	 * 
	 */
	public void setDownlight() {
		bubble.addStyleName(style.downlight());
		arrowOutline.addStyleName(style.arrowOutlineDownlight());
		arrow.addStyleName(style.arrowDownlight());

	}
}
