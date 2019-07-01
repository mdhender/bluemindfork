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
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.SimplePanel;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.cti.api.Status;
import net.bluemind.cti.api.gwt.endpoint.ComputerTelephonyIntegrationGwtEndpoint;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.ui.common.client.forms.Ajax;
import net.bluemind.ui.im.client.IMConstants;
import net.bluemind.ui.im.client.IMCtrl;
import net.bluemind.ui.im.client.Photo;
import net.bluemind.ui.im.client.RosterItemCache;
import net.bluemind.ui.im.client.push.message.PresenceMessage;
import net.bluemind.user.api.gwt.endpoint.UserGwtEndpoint;

public class Header extends FocusPanel {

	private static HeaderUiBinder uiBinder = GWT.create(HeaderUiBinder.class);

	interface HeaderUiBinder extends UiBinder<FlowPanel, Header> {
	}

	public interface HeaderBundle extends ClientBundle {
		@Source("Header.css")
		HeaderStyle getStyle();
	}

	public interface HeaderStyle extends CssResource {
		public String header();

		public String photo();

		public String displayName();

		public String infos();

		public String statusLabel();

		public String statusDropDownMenu();

		public String statusMenuItem();

		public String statusContainer();
	}

	public static HeaderStyle style;
	public static HeaderBundle bundle;

	private static StatusBundle statusBundle;
	private static StatusStyle statusStyle;

	@UiField
	Photo photo;

	@UiField
	SimplePanel statusIM;

	@UiField
	Label statusPhone;

	@UiField
	FlowPanel infos;

	@UiField
	Label displayName;

	@UiField
	MenuBar statusMenu;

	@UiField
	MenuItem statusMenuItem;

	@UiField
	FlowPanel statusContainer;

	private static final int PRIORITY = 1;

	private String imBubbleStyle;

	public Header() {
		setWidget(uiBinder.createAndBindUi(this));

		bundle = GWT.create(HeaderBundle.class);
		style = bundle.getStyle();
		style.ensureInjected();

		statusBundle = GWT.create(StatusBundle.class);
		statusStyle = statusBundle.getStyle();
		statusStyle.ensureInjected();

		setStyleName(style.header());

		RosterItem ri = new RosterItem();
		ri.name = Ajax.getDisplayName();
		ri.user = Ajax.getDefaultEmail();
		RosterItemCache.getInstance().put(ri.user, ri);

		UserGwtEndpoint ep = new UserGwtEndpoint(Ajax.TOKEN.getSessionId(), Ajax.TOKEN.getContainerUid());
		ep.getPhoto(Ajax.TOKEN.getSubject(), new AsyncHandler<byte[]>() {

			@Override
			public void success(byte[] value) {
				RosterItem ri = new RosterItem();
				ri.name = Ajax.getDisplayName();
				ri.user = Ajax.getDefaultEmail();
				ri.photo = atob(new String(value));
				StringBuilder dataUrl = new StringBuilder();
				dataUrl.append("data:image/jpeg;base64,");
				dataUrl.append(ri.photo);
				photo.setUrl(dataUrl.toString());

				RosterItemCache.getInstance().put(ri.user, ri);
			}

			native String atob(String encoded)
			/*-{
    return atob(encoded);
	}-*/;

			@Override
			public void failure(Throwable e) {
			}

		});

		photo.setSize(32);
		photo.setStyleName(style.photo());

		infos.setStyleName(style.infos());
		// FIXME displayname
		displayName.setText(Ajax.getDisplayName());
		displayName.setStyleName(style.displayName());
		statusMenuItem.setStyleName(style.statusLabel());
		statusContainer.setStyleName(style.statusContainer());

		MenuBar statusSwitcher = new MenuBar(true);
		statusSwitcher.setAutoOpen(false);
		statusSwitcher.setAnimationEnabled(false);

		// AVAILABLE
		statusSwitcher.addItem(menuItemStyle(statusStyle.statusAvailable(), IMConstants.INST.statusAvailable()),
				new Command() {
					@Override
					public void execute() {
						IMCtrl.getInstance().setPresence("available", IMConstants.INST.statusAvailable());
					}
				});
		statusSwitcher.addItem(menuItemStyle(statusStyle.statusAvailable(), IMConstants.INST.customStatusMessage()),
				new Command() {
					@Override
					public void execute() {
						PromptCustomStatus prompt = new PromptCustomStatus(
								IMConstants.INST.customAvailableStatusMessage(), IMConstants.INST.customMessage(),
								PRIORITY);
						prompt.setMode("available");
						prompt.show();
					}
				});
		statusSwitcher.addSeparator();

		// BUSY
		statusSwitcher.addItem(menuItemStyle(statusStyle.statusBusy(), IMConstants.INST.statusBusy()), new Command() {
			@Override
			public void execute() {
				IMCtrl.getInstance().setPresence("dnd", IMConstants.INST.statusBusy());
			}
		});
		statusSwitcher.addItem(menuItemStyle(statusStyle.statusBusy(), IMConstants.INST.customStatusMessage()),
				new Command() {
					@Override
					public void execute() {
						PromptCustomStatus prompt = new PromptCustomStatus(IMConstants.INST.customBusyStatusMessage(),
								IMConstants.INST.customMessage(), PRIORITY);
						prompt.setMode("dnd");
						prompt.show();
					}
				});
		statusSwitcher.addSeparator();

		// AWAY
		statusSwitcher.addItem(menuItemStyle(statusStyle.statusAway(), IMConstants.INST.statusAway()), new Command() {
			@Override
			public void execute() {
				IMCtrl.getInstance().setPresence("away", IMConstants.INST.statusAway());
			}
		});
		statusSwitcher.addItem(menuItemStyle(statusStyle.statusAway(), IMConstants.INST.customStatusMessage()),
				new Command() {
					@Override
					public void execute() {
						PromptCustomStatus prompt = new PromptCustomStatus(IMConstants.INST.customAwayStatusMessage(),
								IMConstants.INST.customMessage(), PRIORITY);
						prompt.setMode("away");
						prompt.show();
					}
				});

		statusMenuItem.setSubMenu(statusSwitcher);

		statusMenu.setStyleName(style.statusDropDownMenu());

		statusMenu.addItem(statusMenuItem);

		imBubbleStyle = statusStyle.status();

		if (Ajax.TOKEN.getRoles().contains(BasicRoles.ROLE_CTI)) {
			statusPhone.setVisible(true);
			ComputerTelephonyIntegrationGwtEndpoint ctiEP = new ComputerTelephonyIntegrationGwtEndpoint(
					Ajax.TOKEN.getSessionId(), Ajax.TOKEN.getContainerUid(), Ajax.TOKEN.getSubject());

			ctiEP.getStatus(new AsyncHandler<Status>() {

				@Override
				public void success(Status value) {
					updatePhoneStatus(Entry.statusFromString(value));
				}

				@Override
				public void failure(Throwable e) {
					setPhoneUnavailable();
				}

			});
		}

		setIMAvailable(IMConstants.INST.statusAvailable());
	}

	/**
	 * @return
	 */
	private SafeHtml menuItemStyle(String css, String label) {
		SafeHtmlBuilder shb = new SafeHtmlBuilder();
		shb.appendHtmlConstant("<div class=\"" + style.statusMenuItem() + " " + css + "\">&nbsp;&nbsp;</div> " + label);
		return shb.toSafeHtml();
	}

	/**
	 * @param msg
	 */
	private void setIMAvailable(String msg) {
		if (msg == null || msg.isEmpty()) {
			msg = IMConstants.INST.statusAvailable();
		}
		statusIM.setStyleName(imBubbleStyle);
		statusIM.addStyleName(statusStyle.statusAvailable());
		statusMenuItem.setText(msg);
	}

	/**
	 * @param msg
	 */
	private void setIMBusy(String msg) {
		if (msg == null || msg.isEmpty()) {
			msg = IMConstants.INST.statusBusy();
		}
		statusIM.setStyleName(imBubbleStyle);
		statusIM.addStyleName(statusStyle.statusBusy());
		statusMenuItem.setText(msg);
	}

	/**
	 * @param msg
	 */
	private void setIMAway(String msg) {
		if (msg == null || msg.isEmpty()) {
			msg = IMConstants.INST.statusAway();
		}
		statusIM.setStyleName(imBubbleStyle);
		statusIM.addStyleName(statusStyle.statusAway());
		statusMenuItem.setText(msg);
	}

	private void setPhoneAvailable() {
		statusPhone.setStyleName(statusStyle.statusPhone());
		statusPhone.addStyleName("fa-phone");
		statusPhone.addStyleName("fa");
		statusPhone.addStyleName(statusStyle.statusPhoneAvailable());
	}

	private void setPhoneBusy() {
		statusPhone.setStyleName(statusStyle.statusPhone());
		statusPhone.addStyleName("fa-phone");
		statusPhone.addStyleName("fa");
		statusPhone.addStyleName(statusStyle.statusPhoneAway()); // red
	}

	private void setPhoneUnavailable() {
		statusPhone.setStyleName(statusStyle.statusPhone());
		statusPhone.addStyleName("fa-phone");
		statusPhone.addStyleName("fa");
		statusPhone.addStyleName(statusStyle.statusPhoneOffline());
	}

	private void setPhoneRinging() {
		statusPhone.setStyleName(statusStyle.statusPhone());
		statusPhone.addStyleName("fa-phone");
		statusPhone.addStyleName("fa");
		statusPhone.addStyleName(statusStyle.statusPhoneRinging()); // red +
																	// blink
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

	public void updatePresence(PresenceMessage p) {
		String mode = p.getOwnMode();
		String status = p.getOwnStatus();

		if ("available".equals(mode)) {
			setIMAvailable(status);
		} else if ("dnd".equals(mode)) {
			setIMBusy(status);
		} else if ("away".equals(mode)) {
			setIMAway(status);
		}
	}
}
