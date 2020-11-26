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
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;

import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.gwt.js.JsItemValue;
import net.bluemind.core.container.model.gwt.serder.ItemValueGwtSerDer;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.gwt.js.JsDomain;
import net.bluemind.domain.api.gwt.serder.DomainGwtSerDer;
import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.mailbox.api.gwt.js.JsMailboxRouting;
import net.bluemind.mailshare.api.gwt.js.JsMailshare;
import net.bluemind.ui.adminconsole.base.ui.DelegationEdit;
import net.bluemind.ui.adminconsole.base.ui.MailAddressTableEditor;
import net.bluemind.ui.adminconsole.directory.mailshare.l10n.MailshareConstants;
import net.bluemind.ui.common.client.errors.ErrorCodeTexts;
import net.bluemind.ui.common.client.forms.StringEdit;
import net.bluemind.ui.common.client.forms.finder.ServerFinder;

public class NewMailshare extends CompositeGwtWidgetElement {

	public static final String TYPE = "bm.ac.QCreateMailshareWidget";

	private static NewMailshareUiBinder uiBinder = GWT.create(NewMailshareUiBinder.class);

	interface NewMailshareUiBinder extends UiBinder<HTMLPanel, NewMailshare> {

	}

	private HTMLPanel dlp;

	private ItemValue<Domain> domain;

	@UiField
	DelegationEdit delegation;

	@UiField
	StringEdit name;

	@UiField
	ListBox mailBackend;

	@UiField
	HTMLPanel mailBackendPanel;

	@UiField
	Label errorLabel;

	@UiField
	MailAddressTableEditor mailTable;

	@UiField
	CheckBox mailperms;

	ServerFinder serverFinder = new ServerFinder("mail/imap");

	@UiHandler("mailperms")
	void clickMailPerms(ClickEvent e) {
		boolean mail = mailperms.getValue();
		if (mail) {
			mailTable.asWidget().setValue(name.asEditor().getValue(), domain.value.defaultAlias);
		} else {
			mailTable.asWidget().reset();
		}
		mailTable.setVisible(mailperms.getValue());
	}

	private NewMailshare() {
		dlp = uiBinder.createAndBindUi(this);
		initWidget(dlp);

		// needed to embed a docklayoutpanel
		dlp.setHeight("100%");
		name.setId("new-mailshare-name");
		name.addValueChangeHandler(evt -> {
			mailTable.asWidget().setDefaultLogin(evt.getValue());
			if (this.domain != null) {
				mailTable.asWidget().setValue(evt.getValue(), domain.value.defaultAlias);
			}
		});
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();

		if (map.get("domain") != null) {
			JsItemValue<JsDomain> jsdomain = map.get("domain").cast();

			domain = new ItemValueGwtSerDer<>(new DomainGwtSerDer()).deserialize(new JSONObject(jsdomain));
			mailTable.setDomain(domain);
			mailTable.setVisible(false);
			if (domain.value.global) {
				errorLabel.setText(ErrorCodeTexts.INST.getString("NOT_IN_GLOBAL_DOMAIN"));
			} else {
				errorLabel.setText("");
			}
			delegation.setDomain(domain.uid);
		}
		JsMailshare mailshare = map.get("mailshare").cast();
		name.asEditor().setValue(mailshare.getName());
		mailTable.asEditor().setValue(mailshare.getEmails());
		serverFinder.find(domain.uid, mailBackend, mailBackendPanel);
	}

	@Override
	public void saveModel(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();

		JsMailshare mailshare = map.get("mailshare").cast();
		mailshare.setName(name.asEditor().getValue());
		mailshare.setEmails(mailTable.asEditor().getValue());
		mailshare.setArchived(false);
		mailshare.setRouting(JsMailboxRouting.create(Routing.internal));
		mailshare.setOrgUnitUid(delegation.asEditor().getValue());
		mailshare.setDataLocation(mailBackend.getSelectedValue());
	}

	@UiFactory
	MailshareConstants getConstants() {
		return MailshareConstants.INST;
	}

	@Override
	public void attach(Element parent) {
		super.attach(parent);
		name.setFocus(true);
	}

	public static void registerType() {
		GwtWidgetElement.register(TYPE, new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

			@Override
			public IGwtWidgetElement create(WidgetElement e) {
				return new NewMailshare();
			}
		});
		GWT.log("bm.ac.QCreateMailshareWidget registred");
	}
}
