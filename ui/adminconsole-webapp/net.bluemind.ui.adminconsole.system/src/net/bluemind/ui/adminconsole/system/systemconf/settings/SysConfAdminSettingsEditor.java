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
package net.bluemind.ui.adminconsole.system.systemconf.settings;

import java.util.HashMap;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.ListBox;

import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.ui.adminconsole.system.domains.edit.general.l10n.LocaleIdTranslation;
import net.bluemind.ui.adminconsole.system.hosts.HostKeys;

public class SysConfAdminSettingsEditor extends CompositeGwtWidgetElement {

	public static final String TYPE = "bm.ac.SysConfAdminSettingsEditor";

	@UiField
	ListBox language;

	private HashMap<String, Integer> languageMapping;

	private static SysConfAdminSettingsUiBinder uiBinder = GWT.create(SysConfAdminSettingsUiBinder.class);

	interface SysConfAdminSettingsUiBinder extends UiBinder<HTMLPanel, SysConfAdminSettingsEditor> {
	}

	protected SysConfAdminSettingsEditor() {
		HTMLPanel panel = uiBinder.createAndBindUi(this);
		initWidget(panel);
		setLanguages();
	}

	public static void registerType() {
		GwtWidgetElement.register(TYPE, (widgetelement) -> {
			return new SysConfAdminSettingsEditor();
		});
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		JsMapStringJsObject mapObj = model.cast();
		String hostLanguage = mapObj.getString(HostKeys.lang.name());
		hostLanguage = null != hostLanguage ? hostLanguage : LocaleIdTranslation.DEFAULT_ID;
		language.setSelectedIndex(languageMapping.get(hostLanguage));
	}

	@Override
	public void saveModel(JavaScriptObject model) {
		((JsMapStringJsObject) model.cast()).putString(HostKeys.lang.name(),
				LocaleIdTranslation.getIdByLanguage(language.getSelectedItemText()));
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
}
