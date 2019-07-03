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
package net.bluemind.ui.adminconsole.system.domains.edit.general;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;

import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.domain.api.gwt.js.JsDomain;
import net.bluemind.domain.api.gwt.serder.DomainGwtSerDer;
import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;
import net.bluemind.ui.admin.client.forms.MultiStringEditContainer;
import net.bluemind.ui.adminconsole.system.SettingsModel;
import net.bluemind.ui.adminconsole.system.domains.DomainKeys;
import net.bluemind.ui.adminconsole.system.domains.edit.general.l10n.LocaleIdTranslation;

public class EditDomainGeneralEditor extends CompositeGwtWidgetElement {

	public static final String TYPE = "bm.ac.EditDomainGeneralEditor";

	@UiField
	TextBox name;

	@UiField
	TextArea description;

	@UiField
	MultiStringEditContainer aliases;

	@UiField
	ListBox language;

	private HashMap<String, Integer> languageMapping;

	private static EditDomainGeneralUiBinder uiBinder = GWT.create(EditDomainGeneralUiBinder.class);

	interface EditDomainGeneralUiBinder extends UiBinder<HTMLPanel, EditDomainGeneralEditor> {
	}

	protected EditDomainGeneralEditor() {
		HTMLPanel panel = uiBinder.createAndBindUi(this);
		initWidget(panel);
		setLanguages();
	}

	private void setLanguages() {
		languageMapping = new HashMap<>();
		String[] availableLocaleNames = LocaleInfo.getAvailableLocaleNames();
		int index = 0;
		for (String availableLanguage : availableLocaleNames) {
			if (!availableLanguage.equalsIgnoreCase("default")) {
				String languageById = LocaleIdTranslation.getLanguageById(availableLanguage);
				languageMapping.put(availableLanguage, index++);
				language.addItem(languageById);
			}
		}
	}

	public static void registerType() {
		GwtWidgetElement.register(TYPE, new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

			@Override
			public IGwtWidgetElement create(WidgetElement e) {
				return new EditDomainGeneralEditor();
			}
		});
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();
		JsDomain jsDomain = map.get(DomainKeys.domain.name()).cast();

		Set<String> domainAliases = new HashSet<String>();
		JsArrayString jsAliases = jsDomain.getAliases();
		for (int i = 0; i < jsAliases.length(); i++) {
			domainAliases.add(jsAliases.get(i));
		}
		aliases.setValues(domainAliases);

		Domain domain = new DomainGwtSerDer().deserialize(new JSONObject(jsDomain));
		name.setText(domain.name);
		description.setText(domain.description);
		String domainLanguage = SettingsModel.domainSettingsFrom(model).get(DomainSettingsKeys.lang.name());
		domainLanguage = null != domainLanguage ? domainLanguage : LocaleIdTranslation.DEFAULT_ID;
		language.setSelectedIndex(languageMapping.get(domainLanguage));

	}

	@Override
	public void saveModel(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();
		JsDomain jsDomain = map.get(DomainKeys.domain.name()).cast();
		jsDomain.setName(name.getText());
		jsDomain.setDescription(description.getText());
		map.put(DomainKeys.domain.name(), jsDomain);

		SettingsModel.domainSettingsFrom(model).putString(DomainSettingsKeys.lang.name(),
				LocaleIdTranslation.getIdByLanguage(language.getSelectedItemText()));
		JSONArray updatedAliasValues = new JSONArray();
		int index = 0;
		for (String alias : aliases.getValues()) {
			if (alias.trim().length() > 0) {
				updatedAliasValues.set(index++, new JSONString(alias));
			}
		}
		map.put(DomainKeys.aliases.name(), updatedAliasValues.getJavaScriptObject());
	}
}
