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

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.TextBox;

import net.bluemind.addressbook.api.gwt.js.JsVCard;
import net.bluemind.addressbook.api.gwt.js.JsVCardIdentification;
import net.bluemind.addressbook.api.gwt.js.JsVCardIdentificationName;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.gwt.js.JsItemValue;
import net.bluemind.core.container.model.gwt.serder.ItemValueGwtSerDer;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.gwt.js.JsDomain;
import net.bluemind.domain.api.gwt.serder.DomainGwtSerDer;
import net.bluemind.group.api.Group;
import net.bluemind.group.api.GroupSearchQuery;
import net.bluemind.group.api.IGroupPromise;
import net.bluemind.group.api.gwt.endpoint.GroupGwtEndpoint;
import net.bluemind.gwtconsoleapp.base.editor.Ajax;
import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.notification.Notification;
import net.bluemind.mailbox.api.gwt.js.JsMailboxRouting;
import net.bluemind.role.api.IRolesPromise;
import net.bluemind.role.api.gwt.endpoint.RolesGwtEndpoint;
import net.bluemind.ui.adminconsole.base.DomainsHolder;
import net.bluemind.ui.adminconsole.base.SubscriptionInfoHolder;
import net.bluemind.ui.adminconsole.base.ui.DelegationEdit;
import net.bluemind.ui.adminconsole.base.ui.MailAddressTable;
import net.bluemind.ui.adminconsole.directory.user.l10n.UserConstants;
import net.bluemind.ui.common.client.errors.ErrorCodeTexts;
import net.bluemind.ui.common.client.forms.finder.ServerFinder;
import net.bluemind.user.api.gwt.js.JsUser;

public class NewUser extends CompositeGwtWidgetElement {

	interface EditUserUiBinder extends UiBinder<HTMLPanel, NewUser> {
	}

	public static final String TYPE = "bm.ac.QCreateUserWidget";

	private static EditUserUiBinder uiBinder = GWT.create(EditUserUiBinder.class);

	private ItemValue<Domain> domain;
	@UiField
	TextBox displayName;
	@UiField
	TextBox firstName;

	@UiField
	TextBox lastName;

	@UiField
	TextBox login;

	@UiField
	DelegationEdit delegation;

	@UiField
	MailAddressTable mailAddressTab;

	@UiField
	ListBox mailBackend;

	@UiField
	CheckBox mailperms;

	@UiField
	CheckBox hidden;

	@UiField
	PasswordTextBox password;

	@UiField
	ListBox perms;

	@UiField
	ListBox accountType;

	@UiField
	HTMLPanel accountPanel;

	@UiField
	HTMLPanel mailBackendPanel;

	@UiField
	Label errorLabel;

	ServerFinder serverFinder = new ServerFinder("mail/imap");

	private NewUser() {
		HTMLPanel dlp = uiBinder.createAndBindUi(this);

		initWidget(dlp);
		firstName.getElement().setId("new-user-firstname");
		lastName.getElement().setId("new-user-lastname");
		displayName.getElement().setId("new-user-displayname");
		login.getElement().setId("new-user-login");
		login.getElement().setAttribute("autocomplete", "new-user-login");
		login.getElement().setAttribute("name", "new-user-login");
		password.getElement().setId("new-user-password");
		password.getElement().setAttribute("autocomplete", "new-user-password");
		password.getElement().setAttribute("name", "new-user-password");
		perms.getElement().setId("new-user-perms");
		mailperms.getElement().setId("new-user-mailperms");
		hidden.getElement().setId("new-user-hidden");

		accountType.getElement().setId("new-user-account-type");

		boolean simpleAccounts = SubscriptionInfoHolder.domainAndSubAllowsSimpleAccounts();
		boolean visioAccounts = SubscriptionInfoHolder.domainAndSubAllowsVisioAccounts();
		GWT.log("ALLOWS: " + visioAccounts);
		accountPanel.setVisible(visioAccounts || simpleAccounts);
		if (simpleAccounts || visioAccounts) {
			accountType.addItem(UserConstants.INST.accountTypeFull(), "FULL");
		}
		if (simpleAccounts) {
			accountType.addItem(UserConstants.INST.accountTypeSimple(), "SIMPLE");
		}
		if (visioAccounts) {
			accountType.addItem(UserConstants.INST.accountTypeVisio(), "VISIO");
		}
		login.addChangeHandler(evt -> mailAddressTab.setDefaultLogin(login.getValue()));
		updateDomainChange(DomainsHolder.get().getSelectedDomain());
	}

	@UiHandler("firstName")
	void firstNameKeyboard(KeyUpEvent e) {
		if (firstName.getText().isEmpty()) {
			displayName.setText(lastName.getText());
		} else {
			displayName.setText(firstName.getText() + " " + lastName.getText());
		}
	}

	@UiHandler("lastName")
	void lastNameKeyboard(KeyUpEvent e) {
		if (firstName.getText().isEmpty()) {
			displayName.setText(lastName.getText());
		} else {
			displayName.setText(firstName.getText() + " " + lastName.getText());
		}
	}

	@UiHandler("login")
	void loginKeyboard(KeyUpEvent e) {
		if (mailperms.getValue()) {
			mailAddressTab.setValue(login.getText(), domain.value.defaultAlias);
		}
	}

	@UiHandler("mailperms")
	void clickMailPerms(ClickEvent e) {
		if (login.getText().isEmpty()) {
			errorLabel.setText(ErrorCodeTexts.INST.getString("LOGIN_NOT_DEFINED"));
			mailperms.setValue(false);
			return;
		}
		errorLabel.setText("");
		if (mailperms.getValue()) {
			updateMailTable(domain);
			mailAddressTab.setValue(login.getText(), domain.value.defaultAlias);
		} else {
			updateMailTable(domain);
		}
	}

	private void updateMailTable(ItemValue<Domain> d) {
		mailperms.setVisible(!d.value.global);
		mailAddressTab.setDomain(d);
		mailAddressTab.setVisible(mailperms.getValue() && !d.value.global);
	}

	public static final class GroupAndRoles {
		public final ItemValue<Group> group;
		public final Set<String> roles;

		public GroupAndRoles(ItemValue<Group> group, Set<String> roles) {
			this.group = group;
			this.roles = roles;
		}

		public boolean isAdmin(Set<String> adminRoles) {
			return roles.stream().anyMatch(a -> adminRoles.contains(a));
		}

	}

	private void updateProfileList(ItemValue<Domain> d) {
		perms.clear();
		IRolesPromise roles = new RolesGwtEndpoint(Ajax.TOKEN.getSessionId()).promiseApi();
		IGroupPromise groups = new GroupGwtEndpoint(Ajax.TOKEN.getSessionId(), d.uid).promiseApi();
		GroupSearchQuery q = GroupSearchQuery.matchProperty("is_profile", "true");

		groups.search(q).thenCompose(value -> {

			List<CompletableFuture<GroupAndRoles>> gr = value.stream()
					.map(g -> (CompletableFuture<GroupAndRoles>) groups.getRoles(g.uid).thenApply(aroles -> {
						return new GroupAndRoles(g, aroles);
					})).collect(Collectors.toList());

			return CompletableFuture.allOf(gr.toArray(new CompletableFuture[0])).thenApply(v -> {
				return gr.stream().map(f -> f.join()).collect(Collectors.toList());
			});

		}).thenCombine(roles.getRoles(), (groupsAndRoles, descriptors) -> {

			Set<String> adminRoles = descriptors.stream().filter(desc -> "administration".equals(desc.categoryId))
					.map(desc -> desc.id).collect(Collectors.toSet());

			Collections.sort(groupsAndRoles, (a, b) -> {
				boolean aAdmin = a.isAdmin(adminRoles);
				boolean bAdmin = b.isAdmin(adminRoles);
				int comp = Boolean.compare(aAdmin, bAdmin);
				return comp == 0 ? a.group.displayName.compareTo(b.group.displayName) : comp;
			});

			perms.clear();
			for (GroupAndRoles group : groupsAndRoles) {
				perms.addItem(group.group.value.name, group.group.uid);
			}
			return null;
		}).exceptionally(e -> {
			Notification.get().reportError(e);
			return null;
		});
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();
		if (map.get("domain") != null) {
			JsItemValue<JsDomain> domain = map.get("domain").cast();

			ItemValue<Domain> d = new ItemValueGwtSerDer<>(new DomainGwtSerDer()).deserialize(new JSONObject(domain));
			updateDomainChange(d);
		}
	}

	@Override
	public void saveModel(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();

		JavaScriptObject t = map.get("user");
		JsUser user = null;
		if (t != null) {
			user = t.cast();
		} else {
			user = JsUser.create();
			map.put("user", user);
		}

		user.setLogin(login.getText());
		user.setPassword(password.getText());
		user.setPasswordMustChange(false);
		user.setPasswordNeverExpires(false);
		user.setHidden(hidden.getValue());
		user.setDataLocation(mailBackend.getSelectedValue());
		if (mailAddressTab.getValue().length() > 0) {
			user.setRouting(JsMailboxRouting.internal());
		} else {
			user.setRouting(JsMailboxRouting.none());
		}
		user.setEmails(mailAddressTab.getValue());

		user.setOrgUnitUid(delegation.asEditor().getValue());
		JsVCard vcard = JsVCard.create();
		vcard.setIdentification(JsVCardIdentification.create());
		vcard.getIdentification().setPhoto(false);
		JsVCardIdentificationName name = JsVCardIdentificationName.create();
		vcard.getIdentification().setName(name);
		user.setContactInfos(vcard);

		name.setFamilyNames(lastName.getText());
		name.setGivenNames(firstName.getText());

		String groupUid = perms.getSelectedValue();
		map.putString("defaultGroup", groupUid);

		String account = accountType.getSelectedValue();
		map.putString("accountType", account);

	}

	@Override
	public void attach(Element parent) {
		super.attach(parent);
		firstName.setFocus(true);
	}

	private void updateDomainChange(ItemValue<Domain> active) {
		this.domain = active;
		updateMailTable(active);
		updateProfileList(active);
		delegation.setDomain(active.uid);
		if (domain.value.global) {
			errorLabel.setText(ErrorCodeTexts.INST.getString("NOT_IN_GLOBAL_DOMAIN"));
		} else {
			errorLabel.setText("");
		}
		serverFinder.find(this.domain.uid, mailBackend, mailBackendPanel);
	}

	public static void registerType() {
		GwtWidgetElement.register(TYPE, new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

			@Override
			public IGwtWidgetElement create(WidgetElement e) {
				return new NewUser();
			}
		});
		GWT.log("bm.ac.QCreateUserWidget registred");

	}

}
