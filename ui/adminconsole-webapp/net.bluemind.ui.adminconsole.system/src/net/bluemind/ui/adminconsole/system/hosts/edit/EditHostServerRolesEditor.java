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
package net.bluemind.ui.adminconsole.system.hosts.edit;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.HTMLPanel;

import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;
import net.bluemind.server.api.gwt.js.JsServer;
import net.bluemind.system.api.gwt.js.JsDomainTemplate;
import net.bluemind.system.api.gwt.js.JsDomainTemplateKind;
import net.bluemind.system.api.gwt.js.JsDomainTemplateTag;
import net.bluemind.ui.adminconsole.base.ui.FieldSetPanel;
import net.bluemind.ui.adminconsole.system.hosts.HostKeys;
import net.bluemind.ui.common.client.forms.Ajax;
import net.bluemind.ui.common.client.forms.BooleanEdit;

public class EditHostServerRolesEditor extends CompositeGwtWidgetElement {

	public static final String TYPE = "bm.ac.EditHostServerRolesEditor";
	@UiField
	HTMLPanel tagsPanel;

	private Map<String, BooleanEdit> tagEditors = new HashMap<>();;

	private static EditHostServerRolesUiBinder uiBinder = GWT.create(EditHostServerRolesUiBinder.class);

	interface EditHostServerRolesUiBinder extends UiBinder<HTMLPanel, EditHostServerRolesEditor> {
	}

	protected EditHostServerRolesEditor() {
		HTMLPanel panel = uiBinder.createAndBindUi(this);
		initWidget(panel);
	}

	public static void registerType() {
		GwtWidgetElement.register(TYPE, new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

			@Override
			public IGwtWidgetElement create(WidgetElement e) {
				return new EditHostServerRolesEditor();
			}
		});
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();
		JsServer server = map.get(HostKeys.server.name()).cast();
		JsDomainTemplate domainTemplate = map.get(HostKeys.domainTemplate.name()).cast();
		setupTagsPanel(server.getTags(), domainTemplate);
	}

	@Override
	public void saveModel(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();
		JsServer server = map.get(HostKeys.server.name()).cast();
		map.put(HostKeys.tags.name(), getEditedTags().getJavaScriptObject());
		map.put(HostKeys.server.name(), server);
	}

	private JSONArray getEditedTags() {
		JSONArray newTags = new JSONArray();
		int index = 0;
		for (String t : tagEditors.keySet()) {
			BooleanEdit be = tagEditors.get(t);
			if (be.getValue()) {
				newTags.set(index++, new JSONString(t));
			}
		}
		return newTags;
	}

	private void setupTagsPanel(JsArrayString serverTags, JsDomainTemplate domainTemplate) {
		JsArray<JsDomainTemplateKind> kinds = domainTemplate.getKinds();
		for (int i = 0; i < kinds.length(); i++) {
			JsDomainTemplateKind kind = kinds.get(i);
			JsArray<JsDomainTemplateTag> tags = kind.getTags();

			// Don't display kind with no tags
			if (tags.length() == 0) {
				continue;
			}

			// Define default lang to EN
			// Index is the position of lang in domain.xml - FR first, EN second
			// Only FR and EN are supported in domain.xml
			int langIndex = null != Ajax.getLang() && Ajax.getLang().toLowerCase().indexOf("fr") != -1 ? 0 : 1;

			FieldSetPanel fsp = new FieldSetPanel();
			fsp.setName(getDescription(kind, langIndex));
			tagsPanel.add(fsp);

			for (int j = 0; j < tags.length(); j++) {
				JsDomainTemplateTag tag = tags.get(j);
				BooleanEdit be = new BooleanEdit(tag.getValue());

				if ("mail/imap".equals(tag.getValue())) {
					continue;
				}

				be.setTitleText(tag.getDescription().getI18n().get(langIndex).getText());
				String uniqueId = tag.getValue();
				be.setId(uniqueId);
				fsp.add(be);
				tagEditors.put(uniqueId, be);
			}
		}

		for (int i = 0; i < serverTags.length(); i++) {
			String serverTag = serverTags.get(i);
			BooleanEdit be = tagEditors.get(serverTag);
			if (be != null) {
				be.setValue(true);
			}
		}
	}

	private String getDescription(JsDomainTemplateKind k, int langIndex) {
		String description = k.getDescription().getI18n().get(langIndex).getText();

		if (description == null || description.isEmpty()) {
			return k.getId();
		}

		return description;
	}
}
