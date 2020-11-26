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
package net.bluemind.ui.adminconsole.directory.mailshare;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
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
import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.mailbox.api.gwt.js.JsMailboxRouting;
import net.bluemind.mailshare.api.gwt.js.JsMailshare;
import net.bluemind.ui.admin.client.forms.QuotaEdit;
import net.bluemind.ui.adminconsole.base.ui.DelegationEdit;
import net.bluemind.ui.adminconsole.base.ui.MailAddressTableEditor;
import net.bluemind.ui.common.client.forms.StringEdit;
import net.bluemind.ui.mailbox.backend.MailBackendServerEdit;

public class MailshareGeneralEditor extends CompositeGwtWidgetElement {

	interface GenralUiBinder extends UiBinder<HTMLPanel, MailshareGeneralEditor> {
	}

	public static final String TYPE = "bm.ac.MailshareGeneralEditor";

	private static GenralUiBinder uiBinder = GWT.create(GenralUiBinder.class);

	private String domainUid;

	@UiField
	StringEdit name;

	@UiField
	DelegationEdit delegation;

	@UiField
	QuotaEdit quota;

	@UiField
	MailBackendServerEdit mailBackend;

	@UiField
	ListBox routing;

	@UiField
	MailAddressTableEditor mailTable;

	@UiField
	CheckBox hidden;

	@UiField
	HTMLPanel mailPanel;

	protected MailshareGeneralEditor() {
		HTMLPanel panel = uiBinder.createAndBindUi(this);
		initWidget(panel);

		routing.addChangeHandler(evt -> routingChanged());
		mailBackend.setActive(false);
	}

	private void routingChanged() {
		boolean noneRouting = routing.getSelectedValue().equals(Routing.none.name());
		if (!noneRouting && mailTable.asEditor().getValue().length() == 0) {
			mailTable.asWidget().setValue(name.asEditor().getValue(), domainUid);
		}
		mailPanel.setVisible(!noneRouting);
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		GWT.log("load MODEL edit general !!!!");
		JsMapStringJsObject map = model.cast();
		domainUid = map.getString("domainUid");

		if (map.get("mailshare") == null) {
			GWT.log("mailshare not found..");
			return;
		}

		ItemValue<Domain> domain = new ItemValueGwtSerDer<>(new DomainGwtSerDer())
				.deserialize(new JSONObject(map.get("domain")));
		JsMailshare mailshare = map.get("mailshare").cast();
		name.asEditor().setValue(mailshare.getName());

		mailBackend.setDirEntryUid(map.getString("entryUid"));
		mailBackend.setDomainUid(domain.uid);
		if (mailshare.getDataLocation() != null) {
			mailBackend.asEditor().setValue(mailshare.getDataLocation());
		}

		quota.asEditor()
				.setValue(mailshare.getQuota() != null ? Integer.parseInt(String.valueOf(mailshare.getQuota())) : null);
		quota.setMailboxAndDomain(map.getString("mailboxUid"), map.getString("domainUid"));

		String splitRelay = map.getString(DomainSettingsKeys.mail_routing_relay.name());

		int ordinal = Routing.valueOf(mailshare.getRouting().value()).ordinal();
		if (null == splitRelay || splitRelay.trim().length() == 0) {
			routing.removeItem(1);
			ordinal = Math.min(ordinal, 1);
		}

		routing.setSelectedIndex(ordinal);

		mailTable.asWidget().setDefaultLogin(name.getStringValue());
		mailTable.setDomain(domain);

		JsArray<JsEmail> emails = mailshare.getEmails();
		mailTable.asEditor().setValue(emails);
		delegation.setDomain(map.getString("domainUid"));
		delegation.asEditor().setValue(mailshare.getOrgUnitUid());

		hidden.setValue(mailshare.getHidden());

		routingChanged();
	}

	@Override
	public void saveModel(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();
		JsMailshare mailshare = map.get("mailshare").cast();
		mailshare.setQuota(quota.asEditor().getValue());
		mailshare.setRouting(JsMailboxRouting.create(Routing.valueOf(routing.getSelectedValue())));
		String newRouting = mailshare.getRouting().toString();
		if (newRouting.equals(Routing.internal.name()) || newRouting.equals(Routing.external.name())) {
			mailshare.setEmails(mailTable.asEditor().getValue());
		} else {
			mailshare.setEmails(JsArray.createArray().cast());
		}
		mailshare.setOrgUnitUid(delegation.asEditor().getValue());
		mailshare.setDataLocation(mailBackend.asEditor().getValue());
		mailshare.setHidden(hidden.getValue());
	}

	public static void registerType() {
		GwtWidgetElement.register(TYPE, new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

			@Override
			public IGwtWidgetElement create(WidgetElement e) {
				return new MailshareGeneralEditor();
			}
		});
	}

}
