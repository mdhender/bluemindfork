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
package net.bluemind.ui.adminconsole.directory.group;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Document;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;

import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.gwt.serder.ItemValueGwtSerDer;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.gwt.serder.DomainGwtSerDer;
import net.bluemind.group.api.gwt.js.JsGroup;
import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;
import net.bluemind.ui.adminconsole.base.ui.MailAddressTableEditor;
import net.bluemind.ui.adminconsole.directory.group.l10n.GroupConstants;
import net.bluemind.ui.mailbox.backend.MailBackendServerEdit;

public class MailboxGroupEditor extends CompositeGwtWidgetElement {

	public static final String TYPE = "bm.ac.MaibloxGroupEditor";

	@UiFactory
	GroupConstants getTexts() {
		return GroupConstants.INST;
	}

	interface MailboxGroupEditorUiBinder extends UiBinder<HTMLPanel, MailboxGroupEditor> {
	}

	private MailboxGroupEditorUiBinder binder = GWT.create(MailboxGroupEditorUiBinder.class);

	@UiField
	MailAddressTableEditor mailTable;

	@UiField
	ListBox messaging;

	@UiField
	CheckBox archiveMail;

	@UiField
	Label emailLabel;

	@UiField
	MailBackendServerEdit mailBackend;

	public MailboxGroupEditor() {

		HTMLPanel dlp = binder.createAndBindUi(this);
		dlp.setHeight("100%");
		initWidget(dlp);
		messaging.addItem(getTexts().noEmail());
		messaging.addItem(getTexts().blueMind());
		archiveMail.addValueChangeHandler((event) -> {
			getElement().dispatchEvent(Document.get().createHtmlEvent("refresh", true, true));
		});
	}

	@Override
	public void saveModel(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();

		JsGroup group = map.get("group").cast();
		group.setMailArchived(archiveMail.asEditor().getValue());
		group.setEmails(mailTable.asEditor().getValue());
		group.setDataLocation(mailBackend.asEditor().getValue());
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();
		JsGroup group = map.get("group").cast();
		archiveMail.asEditor().setValue(group.getMailArchived());

		ItemValue<Domain> domain = new ItemValueGwtSerDer<>(new DomainGwtSerDer())
				.deserialize(new JSONObject(map.get("domain")));
		mailTable.setDomain(domain);
		mailTable.asWidget().setDefaultLogin(group.getName().toLowerCase());

		if (group.getEmails().length() == 0) {
			messaging.setSelectedIndex(0);
			mailTable.asWidget().setReadOnly(true);
			mailTable.setVisible(false);
			emailLabel.setVisible(false);
			archiveMail.setVisible(false);
		} else {
			mailTable.asEditor().setValue(group.getEmails());
			messaging.setSelectedIndex(1);
			mailTable.asWidget().setReadOnly(false);
			mailTable.setVisible(true);
			emailLabel.setVisible(true);
			archiveMail.setVisible(true);
		}

		mailBackend.setDirEntryUid(map.getString("entryUid"));
		mailBackend.setDomainUid(domain.uid);
		if (group.getDataLocation() != null) {
			mailBackend.asEditor().setValue(group.getDataLocation());
		}
		messaging.addChangeHandler(evt -> {
			if (messaging.getSelectedIndex() == 1) {
				mailTable.asWidget().setValue(group.getName().toLowerCase(), "all");
				mailTable.setVisible(true);
				emailLabel.setVisible(true);
				archiveMail.setVisible(true);
			} else {
				mailTable.asWidget().reset();
				mailTable.setVisible(false);
				emailLabel.setVisible(false);
				archiveMail.setVisible(false);
			}
		});
	}

	public static void registerType() {
		GwtWidgetElement.register(TYPE, new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

			@Override
			public IGwtWidgetElement create(WidgetElement e) {
				return new MailboxGroupEditor();
			}
		});
	}
}
