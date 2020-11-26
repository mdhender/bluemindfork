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
package net.bluemind.ui.adminconsole.directory.resource;

import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;

import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.gwt.js.JsItemValue;
import net.bluemind.core.container.model.gwt.serder.ItemValueGwtSerDer;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.gwt.js.JsDomain;
import net.bluemind.domain.api.gwt.serder.DomainGwtSerDer;
import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;
import net.bluemind.resource.api.gwt.js.JsResourceDescriptor;
import net.bluemind.resource.api.gwt.js.JsResourceDescriptorPropertyValue;
import net.bluemind.ui.admin.client.forms.TextEdit;
import net.bluemind.ui.adminconsole.base.ui.DelegationEdit;
import net.bluemind.ui.adminconsole.base.ui.MailAddressTableEditor;
import net.bluemind.ui.adminconsole.base.ui.UserOrGroupEntityEdit;
import net.bluemind.ui.adminconsole.directory.resource.l10n.ResourceConstants;
import net.bluemind.ui.common.client.errors.ErrorCodeTexts;
import net.bluemind.ui.common.client.forms.StringEdit;
import net.bluemind.ui.common.client.forms.finder.ServerFinder;

public class NewResource extends CompositeGwtWidgetElement {

	public static final String TYPE = "bm.ac.QCreateResourceWidget";

	private static NewResourceUiBinder uiBinder = GWT.create(NewResourceUiBinder.class);

	interface NewResourceUiBinder extends UiBinder<HTMLPanel, NewResource> {

	}

	@UiField
	DelegationEdit delegation;

	@UiField
	StringEdit name;

	@UiField
	TextEdit desc;

	@UiField
	Label errorLabel;

	@UiField
	MailAddressTableEditor mailTable;

	@UiField
	UserOrGroupEntityEdit ugEdit;

	@UiField
	ResourceTypeCombo type;

	@UiField
	HTMLPanel mailBackendPanel;

	@UiField
	ListBox mailBackend;

	ServerFinder serverFinder = new ServerFinder("mail/imap");

	private ItemValue<Domain> domain;

	public NewResource() {
		HTMLPanel dlp = uiBinder.createAndBindUi(this);
		initWidget(dlp);

		// needed to embed a docklayoutpanel
		dlp.setHeight("100%");

		desc.getElement().setId("new-resource-description");
		name.setId("new-resource-name");
		name.addValueChangeHandler(evt -> {
			// This is not really required, but hey, why not?
			mailTable.asWidget().setDefaultLogin(evt.getValue());
			if (this.domain != null) {
				mailTable.asWidget().setValue(evt.getValue(), domain.value.defaultAlias);
			}
		});
	}

	private void updateDomainChange(ItemValue<Domain> active) {
		this.domain = active;
		mailTable.setDomain(domain);
		ugEdit.setDomain(active);
		mailTable.setVisible(!domain.value.global);
		type.init(active.uid);
		delegation.setDomain(active.uid);
		if (domain.value.global) {
			errorLabel.setText(ErrorCodeTexts.INST.getString("NOT_IN_GLOBAL_DOMAIN"));
		} else {
			errorLabel.setText("");
		}
		serverFinder.find(this.domain.uid, mailBackend, mailBackendPanel);
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();
		if (map.get("domain") != null) {
			JsItemValue<JsDomain> jsdomain = map.get("domain").cast();
			ItemValue<Domain> d = new ItemValueGwtSerDer<>(new DomainGwtSerDer()).deserialize(new JSONObject(jsdomain));
			updateDomainChange(d);
		}
	}

	@Override
	public void saveModel(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();

		JsResourceDescriptor rd = map.get("rd").cast();
		rd.setLabel(name.asEditor().getValue());
		rd.setTypeIdentifier(type.getSelectedValue());
		rd.setDescription(desc.asEditor().getValue());
		rd.setEmails(mailTable.asEditor().getValue());
		rd.setDataLocation(mailBackend.getSelectedValue());
		rd.setProperties(JsArray.createArray().<JsArray<JsResourceDescriptorPropertyValue>>cast());
		rd.setOrgUnitUid(delegation.asEditor().getValue());
		JsArrayString admins = JavaScriptObject.createArray().cast();
		Set<DirEntry> values = ugEdit.getValues();
		int index = 0;
		for (DirEntry dirEntry : values) {
			admins.set(index++, dirEntry.entryUid);
		}
		map.put("res-admin-uids", admins);
	}

	@UiFactory
	ResourceConstants getConstants() {
		return ResourceConstants.INST;
	}

	public static void registerType() {
		GwtWidgetElement.register(TYPE, new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

			@Override
			public IGwtWidgetElement create(WidgetElement e) {
				return new NewResource();
			}
		});
		GWT.log("bm.ac.QCreateResourceWidget registred");
	}
}
