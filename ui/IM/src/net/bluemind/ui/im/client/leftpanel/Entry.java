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
package net.bluemind.ui.im.client.leftpanel;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.cti.api.Status;
import net.bluemind.cti.api.Status.PhoneState;
import net.bluemind.cti.api.gwt.endpoint.ComputerTelephonyIntegrationGwtEndpoint;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.ui.common.client.forms.Ajax;
import net.bluemind.ui.im.client.IMConstants;
import net.bluemind.ui.im.client.IMCtrl;
import net.bluemind.ui.im.client.Photo;

public class Entry extends FocusPanel {

	private static EntryUiBinder uiBinder = GWT.create(EntryUiBinder.class);

	interface EntryUiBinder extends UiBinder<FlowPanel, Entry> {
	}

	public interface EntryBundle extends ClientBundle {
		@Source("Entry.css")
		EntryStyle getStyle();
	}

	public interface EntryStyle extends CssResource {
		public String entry();

		public String offline();

		public String action();

		public String photoContainer();

		public String displayName();

		public String statusLabel();

		public String infos();

		public String statusUnknownLabel();
	}

	private static EntryStyle style;
	private static EntryBundle bundle;

	private static StatusBundle statusBundle;
	private static StatusStyle statusStyle;

	private ClickHandler createConversationClickHandler;
	private HandlerRegistration handler;

	private String jabberId;
	private String latd;

	@UiField
	Photo photo;

	@UiField
	SimplePanel statusIM;

	@UiField
	Label statusPhone;

	@UiField
	Label subscriptionAction;

	@UiField
	SimplePanel photoContainer;

	@UiField
	FlowPanel infos;

	@UiField
	Label displayName;

	@UiField
	Label statusLabel;

	@UiField
	Label resendSubscriptionRequest;

	private String imBubbleStyle;

	public Entry(RosterItem ri) {
		setWidget(uiBinder.createAndBindUi(this));

		subscriptionAction.setVisible(false);
		setSubscriptionActionMouseHandler();

		jabberId = ri.user;
		latd = ri.latd;

		bundle = GWT.create(EntryBundle.class);
		style = bundle.getStyle();
		style.ensureInjected();

		statusBundle = GWT.create(StatusBundle.class);
		statusStyle = statusBundle.getStyle();
		statusStyle.ensureInjected();

		setStyleName(style.entry());
		photoContainer.setStyleName(style.photoContainer());

		infos.setStyleName(style.infos());
		displayName.setStyleName(style.displayName());
		statusLabel.setStyleName(style.statusLabel());

		if (createConversationClickHandler == null) {
			createConversationClickHandler = new ClickHandler() {

				@Override
				public void onClick(ClickEvent event) {
					event.preventDefault();
					IMCtrl.getInstance().createChat(jabberId);
				}
			};
			addClickHandler(createConversationClickHandler);
		}

		imBubbleStyle = statusStyle.status();

		displayName.setText(ri.name);
		displayName.setTitle(ri.latd);

		if (ri.photo != null) {
			StringBuilder dataUrl = new StringBuilder();
			dataUrl.append("data:image/jpeg;base64,");
			dataUrl.append(ri.photo);
			photo.set(dataUrl.toString(), 24);
		}

		if (Ajax.TOKEN.getRoles().contains(BasicRoles.ROLE_CTI)) {
			statusPhone.setVisible(true);

			if (ri.userUid != null) {
				ComputerTelephonyIntegrationGwtEndpoint ctiEP = new ComputerTelephonyIntegrationGwtEndpoint(
						Ajax.TOKEN.getSessionId(), Ajax.TOKEN.getContainerUid(), ri.userUid);

				ctiEP.getStatus(new AsyncHandler<Status>() {

					@Override
					public void success(Status value) {
						updatePhoneStatus(statusFromString(value));
					}

					@Override
					public void failure(Throwable e) {
						setPhoneUnavailable();
					}

				});
			}
		}

		setUnsubscribeAction();

		if ("none".equals(ri.subs) || "from".equals(ri.subs)) {
			setIMUnknown();
		} else {
			setIMOffline();
		}

	}

	private void setSubscriptionActionMouseHandler() {
		this.addMouseOverHandler(new MouseOverHandler() {

			@Override
			public void onMouseOver(MouseOverEvent event) {
				subscriptionAction.setVisible(true);
			}
		});
		this.addMouseOutHandler(new MouseOutHandler() {

			@Override
			public void onMouseOut(MouseOutEvent event) {
				subscriptionAction.setVisible(false);
			}
		});
	}

	public void updatePresence(RosterItem p) {

		if ("unavailable".equals(p.subscriptionType)) {
			setIMOffline();
		} else {
			String mode = p.mode;
			if (mode == null) {
				setIMAvailable(p.status);
			} else if ("dnd".equals(mode)) {
				setIMBusy(p.status);
			} else if ("away".equals(mode)) {
				setIMAway(p.status);
			}
		}

	}

	private void setIMAvailable(String st) {
		resendSubscriptionRequest.setVisible(false);
		setStyleName(style.entry());
		statusIM.setStyleName(imBubbleStyle);
		statusIM.addStyleName(statusStyle.statusAvailable());
		if (st != null && !st.isEmpty()) {
			statusLabel.setText(st);
		} else {
			statusLabel.setText(IMConstants.INST.statusAvailable());
		}
	}

	private void setIMAway(String st) {
		resendSubscriptionRequest.setVisible(false);
		setStyleName(style.entry());
		statusIM.setStyleName(imBubbleStyle);
		statusIM.addStyleName(statusStyle.statusAway());
		if (st != null && !st.isEmpty()) {
			statusLabel.setText(st);
		} else {
			statusLabel.setText(IMConstants.INST.statusAway());
		}
	}

	private void setIMBusy(String st) {
		resendSubscriptionRequest.setVisible(false);
		setStyleName(style.entry());
		statusIM.setStyleName(imBubbleStyle);
		statusIM.addStyleName(statusStyle.statusBusy());
		if (st != null && !st.isEmpty()) {
			statusLabel.setText(st);
		} else {
			statusLabel.setText(IMConstants.INST.statusBusy());
		}
	}

	public void setIMOffline() {
		setStyleName(style.offline());
		statusIM.setStyleName(imBubbleStyle);
		statusIM.addStyleName(statusStyle.statusOffline());
		statusLabel.setStyleName(style.statusLabel());
		statusLabel.setText(IMConstants.INST.statusOffline());
	}

	public void setIMUnknown() {
		setStyleName(style.offline());
		statusIM.setStyleName(imBubbleStyle);
		statusIM.addStyleName(statusStyle.statusOffline());
		statusLabel.setStyleName(style.statusLabel());
		statusLabel.setText(IMConstants.INST.statusUnknown());

		resendSubscriptionRequest.setVisible(true);
		resendSubscriptionRequest.setStyleName(style.statusUnknownLabel());
		resendSubscriptionRequest.setText(IMConstants.INST.resendSubscriptionRequest());
		resendSubscriptionRequest.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				event.stopPropagation();
				event.preventDefault();
				IMCtrl.getInstance().addBuddy(jabberId);
				resendSubscriptionRequest.setVisible(false);
			}
		});

	}

	private void setPhoneUnavailable() {
		statusPhone.setStyleName(statusStyle.statusPhone());
		statusPhone.addStyleName(statusStyle.statusPhoneOffline());
		statusPhone.addStyleName("fa-phone");
		statusPhone.addStyleName("fa");
	}

	private void setPhoneAvailable() {
		statusPhone.setStyleName(statusStyle.statusPhone());
		statusPhone.addStyleName(statusStyle.statusPhoneAvailable());
		statusPhone.addStyleName("fa-phone");
		statusPhone.addStyleName("fa");
	}

	private void setPhoneBusy() {
		statusPhone.setStyleName(statusStyle.statusPhone());
		statusPhone.addStyleName(statusStyle.statusPhoneAway()); // red
		statusPhone.addStyleName("fa-phone");
		statusPhone.addStyleName("fa");
	}

	private void setPhoneRinging() {
		statusPhone.setStyleName(statusStyle.statusPhone());
		statusPhone.addStyleName(statusStyle.statusPhoneRinging()); // red +
		statusPhone.addStyleName("fa-phone");
		statusPhone.addStyleName("fa");
		// blink
	}

	/**
	 * 
	 */
	public void setSubscribeAction() {
		if (handler != null) {
			handler.removeHandler();
		}
		subscriptionAction.setStyleName("fa fa-star star-small");
		subscriptionAction.setTitle(IMConstants.INST.addToFavorites(jabberId));
		handler = subscriptionAction.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				event.stopPropagation();
				event.preventDefault();

				IMCtrl.getInstance().showAddToFavoritesScreen(jabberId);

			}
		});
	}

	private void setUnsubscribeAction() {
		if (handler != null) {
			handler.removeHandler();
		}
		subscriptionAction.setStyleName("fa fa-star star-small");
		subscriptionAction.setTitle(IMConstants.INST.removeFromFavorites(jabberId));
		handler = subscriptionAction.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				event.stopPropagation();
				event.preventDefault();
				IMCtrl.getInstance().showRemoveFromFavoritesScreen(jabberId);
			}
		});
	}

	public String getFullName() {
		return jabberId;
	}

	public String getLatd() {
		return latd;
	}

	public static String statusFromString(Status status) {
		if (status.phoneState == PhoneState.Unknown) {
			return "UNKNOWN";
		} else if (status.phoneState == PhoneState.Ringing) {
			return "RINGING";
		} else {
			return status.type.code();
		}
	}

	public void updatePhoneStatus(String status) {
		if ("RINGING".equals(status)) {
			setPhoneRinging();
		} else if ("BUSY".equals(status) || "CALLING".equals(status)) {
			setPhoneBusy();
		} else if ("AVAILABLE".equals(status)) {
			setPhoneAvailable();
		} else {
			setPhoneUnavailable();
		}
	}
}
