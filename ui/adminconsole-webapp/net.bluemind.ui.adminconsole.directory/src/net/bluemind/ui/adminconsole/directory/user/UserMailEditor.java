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

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.ListBox;

import net.bluemind.core.api.gwt.js.JsEmail;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.gwt.serder.ItemValueGwtSerDer;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.domain.api.gwt.serder.DomainGwtSerDer;
import net.bluemind.gwtconsoleapp.base.editor.ContainerElement;
import net.bluemind.gwtconsoleapp.base.editor.ScreenElement;
import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtContainerElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtContainerElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.mailbox.api.gwt.js.JsMailboxRouting;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.ui.admin.client.forms.QuotaEdit;
import net.bluemind.ui.adminconsole.base.ui.MailAddressTableEditor;
import net.bluemind.ui.adminconsole.directory.user.l10n.UserConstants;
import net.bluemind.ui.common.client.forms.StringEdit;
import net.bluemind.ui.mailbox.backend.MailBackendServerEdit;
import net.bluemind.ui.mailbox.filter.MailForwardEditor;
import net.bluemind.ui.mailbox.filter.SieveEdit;
import net.bluemind.ui.mailbox.identity.UserIdentityManagement;
import net.bluemind.ui.mailbox.vacation.MailVacationEditor;
import net.bluemind.user.api.gwt.js.JsUser;

public class UserMailEditor extends GwtContainerElement {

	private static UserMailUiBinder uiBinder = GWT.create(UserMailUiBinder.class);

	interface UserMailUiBinder extends UiBinder<HTMLPanel, UserMailEditor> {
	}

	@UiField
	ListBox mailRoutingSel;

	@UiField
	HTMLPanel mailFieldsets;

	@UiField
	HTMLPanel ext;

	@UiField
	HTMLPanel noMailFieldsets;

	@UiField
	HTMLPanel extMailFieldsets;

	@UiField
	MailBackendServerEdit mailBackend;

	@UiField
	QuotaEdit quota;

	@UiField
	MailAddressTableEditor mailTable;

	@UiField
	MailAddressTableEditor extMailTable;

	@UiField
	StringEdit customEmail;

	@UiField
	CheckBox hidden;

	private String login;

	protected UserMailEditor(ContainerElement model) {
		super(model);
		HTMLPanel panel = uiBinder.createAndBindUi(this);
		initWidget(panel);
		mailRoutingSel.addChangeHandler(evt -> routingChanged());
	}

	protected void routingChanged() {
		String routing = mailRoutingSel.getSelectedValue();
		if (routing.equals(Routing.internal.name())) {
			mailFieldsets.setVisible(true);
			extMailFieldsets.setVisible(false);
			noMailFieldsets.setVisible(false);
			addDefaultMail();
		} else if (routing.equals(Routing.external.name())) {
			mailFieldsets.setVisible(false);
			extMailFieldsets.setVisible(true);
			noMailFieldsets.setVisible(false);
			addDefaultMail();
		}
	}

	private void addDefaultMail() {
		if (login != null) {
			mailTable.asWidget().setDefaultLogin(login);
			mailTable.asWidget().setValue(login, "all");
			extMailTable.asWidget().setDefaultLogin(login);
			extMailTable.asWidget().setValue(login, "all");
		}
	}

	@Override
	protected void attachChild(WidgetElement widgetElement) {
		if (widgetElement.getTitle() != null) {
			Element div = DOM.createDiv();
			div.setInnerText(widgetElement.getTitle());
			div.addClassName("sectionTitle");
			ext.getElement().appendChild(div);
		}
		widgetElement.attach(ext.getElement());
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		login = null;
		JsMapStringJsObject map = model.cast();
		JsUser user = map.get("user").cast();
		login = user.getLogin();

		String splitRelay = map.getString(DomainSettingsKeys.mail_routing_relay.name());

		int ordinal = Routing.valueOf(user.getRouting().value()).ordinal();
		if (null == splitRelay || splitRelay.trim().length() == 0) {
			mailRoutingSel.removeItem(1);
			ordinal = Math.min(ordinal, 1);
		}

		mailRoutingSel.setSelectedIndex(ordinal);

		mailBackend.setDirEntryUid(map.getString("entryUid"));
		mailBackend.setDomainUid(map.getString("domainUid"));
		if (user.getDataLocation() != null) {
			mailBackend.asEditor().setValue(user.getDataLocation());
		}

		quota.asEditor().setValue(user.getQuota() != null ? Integer.parseInt(String.valueOf(user.getQuota())) : null);
		quota.setMailboxAndDomain(map.getString("userId"), map.getString("domainUid"));

		ItemValue<Domain> domain = new ItemValueGwtSerDer<>(new DomainGwtSerDer())
				.deserialize(new JSONObject(map.get("domain")));
		mailTable.setDomain(domain);
		extMailTable.setDomain(domain);
		GWT.log("[ume] loadModel domain:" + domain + " login: " + login);
		routingChanged();

		mailTable.asEditor().setValue(prepareUserEmails(user));
		extMailTable.asEditor().setValue(prepareUserEmails(user));
		mailTable.asWidget().setDefaultLogin(login);
		extMailTable.asWidget().setDefaultLogin(login);
		hidden.setValue(user.getHidden());
	}

	private JsArray<JsEmail> prepareUserEmails(JsUser user) {
		List<JsEmail> preparedMails = new ArrayList<>();
		JsArray<JsEmail> userMails = user.getEmails();

		for (int i = 0; i < userMails.length(); i++) {
			preparedMails.add(userMails.get(i));
		}

		JsArray<JsEmail> mapMail = JsArray.createArray().cast();
		for (JsEmail email : preparedMails) {
			mapMail.push(email);
		}
		return mapMail;
	}

	@Override
	public void saveModel(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();
		JsUser user = map.get("user").cast();

		user.setRouting(JsMailboxRouting.create(Routing.valueOf(mailRoutingSel.getSelectedValue())));
		user.setDataLocation(mailBackend.asEditor().getValue());

		user.setQuota(quota.asEditor().getValue());
		user.setHidden(hidden.getValue());
		if (user.getRouting().toString().equals(Routing.internal.name())) {
			JsArray<JsEmail> value = mailTable.asEditor().getValue();
			user.setEmails(value);
		} else if (user.getRouting().toString().equals(Routing.external.name())) {
			JsArray<JsEmail> value = extMailTable.asEditor().getValue();
			user.setEmails(value);
		} else {
			JsArray<JsEmail> array = JavaScriptObject.createArray().cast();
			if (null != customEmail.getStringValue()) {
				JsEmail email = JsEmail.create();
				email.setAddress(customEmail.getStringValue());
				email.setIsDefault(true);
				email.setAllAliases(false);
				array.push(email);
			}
			user.setEmails(array);
		}

	}

	public static void registerType() {
		GwtContainerElement.register("bm.ac.UserMailEditor",
				new IGwtDelegateFactory<IGwtContainerElement, ContainerElement>() {

					@Override
					public IGwtContainerElement create(ContainerElement e) {
						return new UserMailEditor(e);
					}
				});
	}

	public static ScreenElement model() {

		JsArray<ScreenElement> children = JsArray.createArray().cast();
		children.push(ScreenElement.create(null, UserIdentityManagement.TYPE)
				.withRole(BasicRoles.ROLE_MANAGE_USER_MAIL_IDENTITIES));
		children.push(
				ScreenElement.create(null, MailboxSharingEditor.TYPE).witTitle(UserConstants.INST.mailboxSharing()));
		children.push(
				ScreenElement.create(null, MailForwardEditor.TYPE).withRole(BasicRoles.ROLE_MANAGE_MAILBOX_FILTER));
		children.push(
				ScreenElement.create(null, MailVacationEditor.TYPE).withRole(BasicRoles.ROLE_MANAGE_MAILBOX_FILTER));
		children.push(ScreenElement.create(null, SieveEdit.TYPE).withRole(BasicRoles.ROLE_MANAGE_MAILBOX_FILTER));

		// FIXME use "native ContainerElement"
		return ContainerElement.createWithType("editUserMail", "bm.ac.UserMailEditor", children);
	}

}
