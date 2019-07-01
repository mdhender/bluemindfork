/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.ui.settings.client.myaccount.external;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.TextBox;

import net.bluemind.ui.settings.client.myaccount.external.l10n.ExternalAccountsEditConstants;

public class AccountQCreateScreen extends PopupPanel {

	private DockLayoutPanel dlp;

	@UiField
	Label icon;

	@UiField
	ListBox accountSelect;

	@UiField
	SpanElement title;

	@UiField
	HTMLPanel content;

	@UiField
	TextBox loginBox;

	@UiField
	TextBox credentialsBox;

	@UiField
	Button cancel;

	@UiField
	Button save;

	@UiHandler("cancel")
	public void handleCancel(ClickEvent e) {
		this.hide();
	}

	@UiHandler("save")
	public void handleSave(ClickEvent e) {
		boolean create = login == null;
		String newLogin = loginBox.getText();
		String credentials = credentialsBox.getText();
		if (newLogin.length() > 0 && credentials.length() > 0) {
			parent.save(login, create, accountSelect.getSelectedItemText(), newLogin, credentials,
					Collections.emptyMap());
			handleCancel(e);
		} else {
			Window.alert(constants.missingValues());
		}
	}

	interface AccountQCreateScreenUiBinder extends UiBinder<DockLayoutPanel, AccountQCreateScreen> {
	}

	private static AccountQCreateScreenUiBinder uiBinder = GWT.create(AccountQCreateScreenUiBinder.class);

	ExternalAccountsEditConstants constants = GWT.create(ExternalAccountsEditConstants.class);

	private ExternalAccountsWidget parent;
	private String login;

	public AccountQCreateScreen() {
		this.dlp = uiBinder.createAndBindUi(this);
		super.setStyleName("gwt-Popup-DefaultRect");
		dlp.setStyleName("gwt-Popup-DefaultRect");
		super.setWidget(dlp);
		icon.setStyleName("fa fa-lg fa-cloud");
		title.setInnerHTML(constants.configureAccount());
	}

	public AccountQCreateScreen(ExternalAccountsWidget parent, JsCompleteExternalSystem system, String login,
			Map<String, String> loginMap, Set<String> unconfiguredSystems) {
		this();
		this.parent = parent;
		this.login = login;

		if (null == login) {
			// create
			unconfiguredSystems.forEach(sys -> {
				accountSelect.addItem(sys);
			});
		} else {
			// update
			accountSelect.addItem(system.getIdentifier());
			loginBox.setText(login);
		}
	}

}
