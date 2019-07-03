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
package net.bluemind.ui.adminconsole.system.domains.edit.instantmessaging;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HTMLPanel;

import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.gwt.js.JsItemValue;
import net.bluemind.core.container.model.gwt.serder.ItemValueGwtSerDer;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.gwt.serder.DirEntryGwtSerDer;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.domain.api.gwt.js.JsDomain;
import net.bluemind.domain.api.gwt.serder.DomainGwtSerDer;
import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;
import net.bluemind.ui.adminconsole.base.ui.UserOrGroupEntityEdit;
import net.bluemind.ui.adminconsole.system.SettingsModel;
import net.bluemind.ui.adminconsole.system.domains.DomainKeys;

//FIXME to delete 
//https://forge.bluemind.net/jira/browse/BM-8278
public class EditDomainInstantMessagingEditor extends CompositeGwtWidgetElement {

	@UiField
	UserOrGroupEntityEdit authorizedEntities;

	@UiField
	CheckBox publicAuth;

	private static EditDomainInstantMessagingUiBinder uiBinder = GWT.create(EditDomainInstantMessagingUiBinder.class);

	interface EditDomainInstantMessagingUiBinder extends UiBinder<HTMLPanel, EditDomainInstantMessagingEditor> {
	}

	protected EditDomainInstantMessagingEditor() {
		HTMLPanel panel = uiBinder.createAndBindUi(this);
		initWidget(panel);
	}

	public static void registerType() {
		GwtWidgetElement.register("bm.ac.EditDomainInstantMessagingEditor",
				new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

					@Override
					public IGwtWidgetElement create(WidgetElement e) {
						return new EditDomainInstantMessagingEditor();
					}
				});
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();
		JSONArray entities = new JSONArray(map.get(DomainKeys.imEntities.name()));
		String publicAuthString = SettingsModel.domainSettingsFrom(model).get(DomainSettingsKeys.im_public_auth.name());
		if (publicAuthString.equals("true")) {
			publicAuth.setValue(true);
		}
		authorizedEntities.setValues(arraytoDirEntries(entities));
		JsItemValue<JsDomain> jsDomain = map.get(DomainKeys.domainItem.name()).cast();
		ItemValue<Domain> domain = new ItemValueGwtSerDer<>(new DomainGwtSerDer())
				.deserialize(new JSONObject(jsDomain));
		authorizedEntities.setDomain(domain);
	}

	@Override
	public void saveModel(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();
		SettingsModel.domainSettingsFrom(model).putString(DomainSettingsKeys.im_public_auth.name(),
				publicAuth.getValue().toString());
		map.put(DomainKeys.imEntities.name(), entriesToArray(authorizedEntities.getValues()).getJavaScriptObject());
	}

	private JSONArray entriesToArray(Set<DirEntry> values) {
		JSONArray entries = new JSONArray();
		int index = 0;
		for (DirEntry dirEntry : values) {
			entries.set(index++, new DirEntryGwtSerDer().serialize(dirEntry));
		}
		return entries;
	}

	private Collection<DirEntry> arraytoDirEntries(JSONArray entities) {
		List<DirEntry> entries = new ArrayList<>();
		for (int i = 0; i < entities.size(); i++) {
			DirEntry entry = new DirEntryGwtSerDer().deserialize(entities.get(i));
			entries.add(entry);
		}
		return entries;
	}

}
