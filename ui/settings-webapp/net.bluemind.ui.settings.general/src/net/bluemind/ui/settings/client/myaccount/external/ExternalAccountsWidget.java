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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;

import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;
import net.bluemind.ui.common.client.icon.Trash;
import net.bluemind.ui.settings.client.myaccount.external.l10n.ExternalAccountsEditConstants;
import net.bluemind.user.api.gwt.js.JsUserAccountInfo;

public class ExternalAccountsWidget extends CompositeGwtWidgetElement {

	public final static String TYPE = "bm.settings.ExternalUserAccounts";

	@UiField
	FlexTable myAccounts;
	@UiField
	Button addAccount;

	List<AccountInfoEdit> myAccountList;
	Map<String, HandlerRegistration> handlerMap;
	Map<String, Integer> rowMap;
	Map<String, String> logos;
	Map<String, String> loginMap;
	Set<String> deleteAccounts;
	List<JsCompleteExternalSystem> systems;
	Set<String> unconfiguredSystems;

	private static final ExternalAccountsEditConstants constants = GWT.create(ExternalAccountsEditConstants.class);

	public static interface Resources extends ClientBundle {
		@Source("Accounts.css")
		Style editStyle();

	}

	public static interface Style extends CssResource {

		String account_tbl_row();

		String account_tbl_col_logo();

		String account_tbl_col_trash();

		String account_tbl_button();

		String account_tbl_row_identifier();

		String systemIcon();

	}

	private static final Resources res = GWT.create(Resources.class);
	private Style style;

	interface ExternalAccountsUiBinder extends UiBinder<HTMLPanel, ExternalAccountsWidget> {
	}

	private static ExternalAccountsUiBinder uiBinder = GWT.create(ExternalAccountsUiBinder.class);

	public ExternalAccountsWidget() {
		style = res.editStyle();
		style.ensureInjected();
		HTMLPanel panel = uiBinder.createAndBindUi(this);
		initWidget(panel);
		addAccount.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				new AccountClickHandler(ExternalAccountsWidget.this, unconfiguredSystems, loginMap).onClick(event);
			}
		});
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		myAccounts.removeAllRows();
		logos = new HashMap<>();
		handlerMap = new HashMap<>();
		loginMap = new HashMap<>();
		rowMap = new HashMap<>();
		myAccountList = new LinkedList<>();
		deleteAccounts = new HashSet<>();
		unconfiguredSystems = new HashSet<>();
		systems = new ArrayList<>();

		final JsMapStringJsObject map = model.cast();

		JsArray<JsCompleteExternalSystem> externalSystems = map.get("external-systems").cast();
		for (int i = 0; i < externalSystems.length(); i++) {
			JsCompleteExternalSystem system = externalSystems.get(i);
			logos.put(system.getIdentifier(), system.getLogo());
			systems.add(system);
			unconfiguredSystems.add(system.getIdentifier());
		}

		JsArray<JsUserAccountInfo> accounts = map.get("external-accounts").cast();
		for (int i = 0; i < accounts.length(); i++) {
			JsUserAccountInfo account = accounts.get(i);
			myAccountList.add(new AccountInfoEdit(account));
			loginMap.put(account.getLogin(), account.getLogin());
			addAccountEntry(account.getExternalSystemId(), account.getLogin(), myAccounts,
					new Image(logos.get(account.getExternalSystemId())), account.getLogin());
			unconfiguredSystems.remove(account.getExternalSystemId());
		}

		checkAddAccountButton();
	}

	public void save(String login, boolean create, String systemIdentifier, String newLogin, String credentials,
			Map<String, String> config) {
		if (create) {
			JsUserAccountInfo accountInfo = JsUserAccountInfo.create();
			accountInfo.setLogin(newLogin);
			accountInfo.setCredentials(credentials);
			accountInfo.setAdditionalSettings(net.bluemind.core.commons.gwt.JsMapStringString.create(config));
			accountInfo.setExternalSystemId(systemIdentifier);
			myAccountList.add(new AccountInfoEdit(accountInfo, EditMode.CREATE));
			loginMap.put(newLogin, newLogin);
			addAccountEntry(systemIdentifier, newLogin, myAccounts, new Image(logos.get(systemIdentifier)), newLogin);
			unconfiguredSystems.remove(systemIdentifier);
		} else {
			for (int i = 0; i < myAccountList.size(); i++) {
				AccountInfoEdit accountInfo = myAccountList.get(i);
				if (accountInfo.matches(login, systemIdentifier)) {
					accountInfo.update(newLogin, credentials, config);
					int row = i * 2;
					((Label) myAccounts.getWidget(row, 2)).setText(newLogin);
					loginMap.put(newLogin, newLogin);
					loginMap.forEach((k, v) -> {
						if (v.equals(login)) {
							loginMap.put(k, newLogin);
						}
					});
				}
			}
		}

		checkAddAccountButton();
	}

	private void addAccountEntry(String systemIdentifier, String label, FlexTable table, Image icon, String login) {
		Anchor identifier = new Anchor(systemIdentifier);
		Anchor description = new Anchor(label);
		AccountClickHandler handler = new AccountClickHandler(this, getSystem(systemIdentifier), login, loginMap);
		identifier.addClickHandler(handler);
		description.addClickHandler(handler);

		icon.setStyleName(style.systemIcon());
		int row = table.getRowCount();
		rowMap.put(systemIdentifier, row);
		handlerMap.put(systemIdentifier, icon.addClickHandler(handler));
		table.setWidget(row, 0, icon);
		table.setWidget(row, 1, identifier);
		table.setWidget(row, 2, new Label(login));

		table.getFlexCellFormatter().setStyleName(row, 0, style.account_tbl_col_logo());
		table.getFlexCellFormatter().setStyleName(row, 1, style.account_tbl_row_identifier());
		table.getFlexCellFormatter().setStyleName(row, 2, style.account_tbl_row());
		table.getFlexCellFormatter().setStyleName(row, 3, style.account_tbl_button());
		table.getFlexCellFormatter().setStyleName(row, 4, style.account_tbl_col_trash());

		Button edit = new Button(constants.edit());
		edit.setStyleName("button");
		edit.addClickHandler(handler);
		table.setWidget(row, 3, edit);

		Trash delete = new Trash();
		delete.addClickHandler(c -> {
			Iterator<AccountInfoEdit> iter = myAccountList.iterator();
			while (iter.hasNext()) {
				AccountInfoEdit accountInfo = iter.next();
				if (accountInfo.matches(loginMap.get(login), systemIdentifier)) {
					if (accountInfo.mode != EditMode.CREATE) {
						accountInfo.delete();
						deleteAccounts.add(accountInfo.account.getExternalSystemId());
					}
					iter.remove();
					table.removeRow(row);
					unconfiguredSystems.add(accountInfo.account.getExternalSystemId());
					break;
				}
			}
			checkAddAccountButton();
		});
		table.setWidget(row, 4, delete);
	}

	private JsCompleteExternalSystem getSystem(String systemIdentifier) {
		for (JsCompleteExternalSystem system : systems) {
			if (system.getIdentifier().equals(systemIdentifier)) {
				return system;
			}
		}
		return null;
	}

	@SuppressWarnings("incomplete-switch")
	@Override
	public void saveModel(JavaScriptObject model) {

		JsArray<JsUserAccountInfo> created = JsArray.createArray().cast();
		JsArray<JsUserAccountInfo> modified = JsArray.createArray().cast();
		JsArrayString deleted = JsArrayString.createArray().cast();

		int createIndex = 0;
		int modifiedIndex = 0;
		int deletedIndex = 0;
		for (AccountInfoEdit account : myAccountList) {
			switch (account.mode) {
			case CREATE:
				if (deleteAccounts.contains(account.account.getExternalSystemId())) {
					modified.set(modifiedIndex++, account.account);
					deleteAccounts.remove(account.account.getExternalSystemId());
				} else {
					created.set(createIndex++, account.account);
				}
				break;
			case UPDATE:
				modified.set(modifiedIndex++, account.account);
				deleteAccounts.remove(account.account.getExternalSystemId());
				break;
			}
		}

		for (String account : deleteAccounts) {
			deleted.set(deletedIndex++, account);
		}

		final JsMapStringJsObject map = model.cast();
		map.put("ext-accounts-created", created);
		map.put("ext-accounts-modified", modified);
		map.put("ext-accounts-deleted", deleted);
	}

	private void checkAddAccountButton() {
		addAccount.setVisible(unconfiguredSystems.size() > 0);
	}

	public static void registerType() {
		GwtWidgetElement.register(TYPE, new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

			@Override
			public IGwtWidgetElement create(WidgetElement e) {
				return new ExternalAccountsWidget();
			}
		});
	}

	public static class AccountInfoEdit {
		public EditMode mode;
		public final JsUserAccountInfo account;

		public AccountInfoEdit(JsUserAccountInfo account) {
			this(account, EditMode.NONE);
		}

		public AccountInfoEdit(JsUserAccountInfo account, EditMode mode) {
			this.account = account;
			this.mode = mode;
		}

		public void delete() {
			this.mode = EditMode.DELETE;
		}

		public void update(String newLogin, String credentials, Map<String, String> config) {
			if (this.mode != EditMode.CREATE) {
				this.mode = EditMode.UPDATE;
			}
			this.account.setLogin(newLogin);
			this.account.setCredentials(credentials);
			this.account.setAdditionalSettings(net.bluemind.core.commons.gwt.JsMapStringString.create(config));
		}

		public boolean matches(String login, String systemIdentifier) {
			return account.getLogin().equals(login) && account.getExternalSystemId().equals(systemIdentifier);
		}
	}

	public static enum EditMode {
		NONE, CREATE, UPDATE, DELETE
	}

	public static class AccountClickHandler implements ClickHandler {
		private final ExternalAccountsWidget parent;
		private final JsCompleteExternalSystem system;
		private final String login;
		private final Map<String, String> loginMap;
		private final Set<String> unconfiguredSystems;

		public AccountClickHandler(ExternalAccountsWidget parent, JsCompleteExternalSystem system, String login,
				Map<String, String> loginMap, Set<String> unconfiguredSystems) {
			this.parent = parent;
			this.system = system;
			this.login = login;
			this.loginMap = loginMap;
			this.unconfiguredSystems = unconfiguredSystems;
		}

		public AccountClickHandler(ExternalAccountsWidget parent, JsCompleteExternalSystem system, String login,
				Map<String, String> loginMap) {
			this(parent, system, login, loginMap, new HashSet<>());
		}

		public AccountClickHandler(ExternalAccountsWidget parent, Set<String> unconfiguredSystems,
				Map<String, String> loginMap) {
			this(parent, null, null, loginMap, unconfiguredSystems);
		}

		@Override
		public void onClick(ClickEvent event) {
			String loginValue = null != login ? loginMap.get(login) : null;

			PopupPanel config = new AccountQCreateScreen(parent, system, loginValue, loginMap, unconfiguredSystems);
			config.setModal(true);
			config.center();
		}
	};

}
