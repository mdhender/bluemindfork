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
package net.bluemind.ui.adminconsole.directory.user;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;

import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.directory.api.BaseDirEntry.AccountType;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.gwt.js.JsBaseDirEntryAccountType;
import net.bluemind.group.api.Group;
import net.bluemind.gwtconsoleapp.base.editor.Ajax;
import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.gwtconsoleapp.base.notification.Notification;
import net.bluemind.ui.adminconsole.base.Actions;
import net.bluemind.ui.adminconsole.base.SubscriptionInfoHolder;
import net.bluemind.ui.adminconsole.base.ui.DelegationEdit;
import net.bluemind.ui.adminconsole.directory.EditGroupMembership;
import net.bluemind.ui.adminconsole.directory.user.l10n.UserConstants;
import net.bluemind.ui.common.client.SizeHint;
import net.bluemind.ui.common.client.forms.StringEdit;
import net.bluemind.user.api.IUserAsync;
import net.bluemind.user.api.gwt.endpoint.UserGwtEndpoint;
import net.bluemind.user.api.gwt.js.JsUser;

public class UserGeneralEditor extends CompositeGwtWidgetElement implements EventListener {

	public static final String TYPE = "bm.ac.UserGeneralEditor";

	interface GenralUiBinder extends UiBinder<HTMLPanel, UserGeneralEditor> {
	}

	private static GenralUiBinder uiBinder = GWT.create(GenralUiBinder.class);

	@UiField
	StringEdit login;

	@UiField
	DelegationEdit delegation;

	@UiField
	ListBox groupsList;

	@UiField
	CheckBox archive;

	@UiField
	Anchor editGroupMembership;

	@UiField
	HTMLPanel accountType;

	@UiField
	HTMLPanel accountPanel;

	private String domainUid;

	private String userUid;

	protected UserGeneralEditor(WidgetElement model) {
		HTMLPanel panel = uiBinder.createAndBindUi(this);
		initWidget(panel);

		boolean ro = model.isReadOnly();
		login.setReadOnly(ro);
		delegation.setReadOnly(ro);
		delegation.setReadOnly(ro);
		editGroupMembership.setEnabled(!ro);
		archive.setEnabled(!ro);
		editGroupMembership.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				event.preventDefault();

				EditGroupMembership g = new EditGroupMembership(domainUid, userUid, Kind.USER);
				g.registerObserver(UserGeneralEditor.this);
				SizeHint sh = g.getSizeHint();
				g.setSize(sh.getWidth() + "px", sh.getHeight() + "px");

				DialogBox overlay = new DialogBox();
				overlay.addStyleName("dialog");
				g.setOverlay(overlay);
				overlay.setWidget(g);
				overlay.setGlassEnabled(true);
				overlay.setAutoHideEnabled(true);
				overlay.setGlassStyleName("modalOverlay");
				overlay.setModal(true);
				overlay.center();
				overlay.show();
			}
		});
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();
		if (map.get("user") == null) {
			GWT.log("user not found..");
			return;
		}

		domainUid = map.getString("domainUid");
		userUid = map.getString("userId");
		final JsUser user = map.get("user").cast();

		login.asEditor().setValue(user.getLogin());

		archive.setValue(user.getArchived());
		reloadGroups();
		groupsList.setMultipleSelect(true);
		groupsList.setEnabled(false);
		delegation.setDomain(domainUid);
		delegation.asEditor().setValue(user.getOrgUnitUid());

		if (SubscriptionInfoHolder.domainHasSimpleAccounts()) {
			accountPanel.setVisible(true);

			boolean isFull = user.getAccountType() == JsBaseDirEntryAccountType.FULL();
			if (isFull) {
				accountType.add(new Label(UserConstants.INST.accountTypeFull()));
			} else {
				accountType.add(new Label(UserConstants.INST.accountTypeSimple()));

				Anchor a = new Anchor(UserConstants.INST.accountTypeSwitchToFull());
				a.addClickHandler(e -> {
					if (Window.confirm(UserConstants.INST.accountTypeSwitchToFullConfirm())) {
						switchAccountType(AccountType.FULL);
					}
				});

				accountType.add(a);
			}

		}
	}

	private void switchAccountType(AccountType accountType) {
		IUserAsync userService = new UserGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid);
		userService.updateAccountType(userUid, accountType, new DefaultAsyncHandler<Void>() {

			@Override
			public void success(Void value) {
				Notification.get().reportInfo("saved");
				Actions.get().reload();
			}
		});
	}

	@Override
	public void saveModel(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();
		if (map.get("user") == null) {
			GWT.log("user not found..");
			return;
		}

		JsUser user = map.get("user").cast();
		user.setLogin(login.getStringValue());
		user.setArchived(archive.getValue());
		user.setOrgUnitUid(delegation.asEditor().getValue());
	}

	public static void registerType() {
		GwtWidgetElement.register(TYPE, new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

			@Override
			public IGwtWidgetElement create(WidgetElement e) {
				return new UserGeneralEditor(e);
			}
		});
	}

	@Override
	public void onBrowserEvent(Event event) {
		// FIXME ugly, dishonorable
		if (null == event) {
			reloadGroups();
		} else {
			super.onBrowserEvent(event);
		}
	}

	private void reloadGroups() {
		groupsList.clear();

		new UserGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid).memberOf(userUid,
				new DefaultAsyncHandler<List<ItemValue<Group>>>() {

			@Override
			public void success(List<ItemValue<Group>> value) {
				for (ItemValue<Group> itemValue : value) {
					groupsList.addItem(itemValue.displayName);
				}
			}

		});
	}

}
