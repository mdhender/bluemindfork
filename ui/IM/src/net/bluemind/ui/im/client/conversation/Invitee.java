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
import net.bluemind.ui.im.client.Photo;
import net.bluemind.ui.im.client.RosterItemCache;
import net.bluemind.ui.im.client.leftpanel.RosterItem;

public class Invitee extends Composite {

	private static InviteeUiBinder uiBinder = GWT.create(InviteeUiBinder.class);

	interface InviteeUiBinder extends UiBinder<FlowPanel, Invitee> {
	}

	public interface InviteeBundle extends ClientBundle {
		@Source("Invitee.css")
		InviteeStyle getStyle();
	}

	public interface InviteeStyle extends CssResource {
		public String occupant();

		public String remove();
	}

	public static InviteeStyle style;
	public static InviteeBundle bundle;

	@UiField
	Label label;

	@UiField
	Photo photo;

	@UiField
	Label remove;

	private String jabberId;

	public Invitee(String jabberId) {
		initWidget(uiBinder.createAndBindUi(this));

		bundle = GWT.create(InviteeBundle.class);

		style = bundle.getStyle();
		style.ensureInjected();

		setStyleName(style.occupant());

		this.jabberId = jabberId;

		RosterItem ri = RosterItemCache.getInstance().get(jabberId);
		if (ri != null) {
			if (ri.photo != null) {
				StringBuilder dataUrl = new StringBuilder();
				dataUrl.append("data:image/jpeg;base64,");
				dataUrl.append(ri.photo);
				photo.set(dataUrl.toString(), 24);
			}
			label.setText(ri.name);
		} else {
			label.setText(jabberId);
		}

		remove.setTitle(IMConstants.INST.removeFromListButton());
		remove.setStyleName("fa fa-lg fa-close");
	}

	public void addClickHandler(ClickHandler clickHandler) {
		remove.addClickHandler(clickHandler);
	}

	public String getJabberId() {
		return jabberId;
	}

}
