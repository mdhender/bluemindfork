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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.i18n.client.TimeZone;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;

import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.domain.api.gwt.js.JsDomain;
import net.bluemind.domain.api.gwt.serder.DomainGwtSerDer;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.system.api.CertData.CertificateDomainEngine;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.ui.admin.client.forms.MultiStringEditContainer;
import net.bluemind.ui.adminconsole.base.DomainsHolder;
import net.bluemind.ui.adminconsole.system.SettingsModel;
import net.bluemind.ui.adminconsole.system.domains.DomainKeys;
import net.bluemind.ui.adminconsole.system.domains.edit.general.l10n.DateFormatTranslation;
import net.bluemind.ui.adminconsole.system.domains.edit.general.l10n.LocaleIdTranslation;
import net.bluemind.ui.adminconsole.system.domains.edit.general.l10n.TimeFormatTranslation;
import net.bluemind.ui.adminconsole.system.systemconf.auth.SysConfAuthenticationEditor;
import net.bluemind.ui.common.client.forms.Ajax;
import net.bluemind.ui.common.client.forms.GwtTimeZone;

public class EditDomainGeneralEditor extends CompositeGwtWidgetElement {
	private static final String DEFAULT_TZ = "Europe/Paris";

	public static final String TYPE = "bm.ac.EditDomainGeneralEditor";

	@UiField
	ListBox defaultAlias;

	@UiField
	Label name;

	@UiField
	TextArea description;

	@UiField
	MultiStringEditContainer aliases;

	@UiField
	ListBox language;

	@UiField
	ListBox dateFormat;

	@UiField
	ListBox timeFormat;

	@UiField
	ListBox tz;

	@UiField
	TextBox externalUrl;

	@UiField
	TextBox otherUrls;

	@UiField
	ListBox domainList;

	@UiField
	TextBox compositionFont;

	@UiField
	ListBox fallbackFonts;

	private HashMap<String, Integer> languageMapping;

	private HashMap<String, Integer> dateFormatMapping;

	private HashMap<String, Integer> timeFormatMapping;

	private HashMap<String, Integer> tzMapping;

	private HashMap<String, Integer> defaultAliasesMapping;

	private HashMap<String, Integer> fallbackFontMapping;

	private String domainDateFormat;

	private static EditDomainGeneralUiBinder uiBinder = GWT.create(EditDomainGeneralUiBinder.class);

	private static final String DEFAULT_FALLBACK_FONT = "sans-serif";

	interface EditDomainGeneralUiBinder extends UiBinder<HTMLPanel, EditDomainGeneralEditor> {
	}

	protected EditDomainGeneralEditor() {
		HTMLPanel panel = uiBinder.createAndBindUi(this);
		initWidget(panel);
		setLanguages();
		setDateFormat();
		setTimeFormat();
		setTimezone();
		setFallbackFont();
		aliases.addChangeHandler(evt -> setAvailableDefaultAliases());
		aliases.setMinimumLength(48);
	}

	private void setDateFormat() {
		dateFormatMapping = new HashMap<>();
		int index = 0;
		for (Entry<String, String> f : DateFormatTranslation.formats.entrySet()) {
			dateFormat.addItem(f.getValue());
			dateFormatMapping.put(f.getKey(), index++);
		}
	}

	private void setTimeFormat() {
		timeFormatMapping = new HashMap<>();
		int index = 0;
		for (Entry<String, String> f : TimeFormatTranslation.formats.entrySet()) {
			timeFormat.addItem(f.getValue());
			timeFormatMapping.put(f.getKey(), index++);
		}
	}

	private void setFallbackFont() {
		fallbackFontMapping = new HashMap<>();
		fallbackFonts.addItem(DEFAULT_FALLBACK_FONT);
		fallbackFontMapping.put(DEFAULT_FALLBACK_FONT, 0);
		fallbackFonts.addItem("serif");
		fallbackFontMapping.put("serif", 1);
		fallbackFonts.addItem("monospace");
		fallbackFontMapping.put("monospace", 2);
		fallbackFonts.addItem("cursive");
		fallbackFontMapping.put("cursive", 3);
		fallbackFonts.addItem("fantasy");
		fallbackFontMapping.put("fantasy", 4);
	}

	private void setTimezone() {
		tzMapping = new HashMap<>();
		int index = 0;
		for (TimeZone t : GwtTimeZone.INSTANCE.getTimeZones()) {
			tzMapping.put(t.getID(), index++);
			tz.addItem(t.getID(), t.getID());
		}
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

	private void setAvailableDefaultAliases() {
		String currentDefaultAlias = defaultAlias.getSelectedValue();
		int currentDefaultIndex = defaultAlias.getSelectedIndex();
		defaultAlias.clear();
		defaultAliasesMapping = new HashMap<>();
		int index = 0;
		for (String alias : aliases.getValues()) {
			if (!alias.trim().isEmpty()) {
				defaultAliasesMapping.put(alias, index);
				defaultAlias.addItem(alias);
				if (alias.equals(currentDefaultAlias)) {
					// Modified the list (add, remove, the "currentIndex" can be a different entry
					defaultAlias.setSelectedIndex(index);
				} else {
					// We have modified the aliasName, so use the index instead
					defaultAlias.setSelectedIndex(currentDefaultIndex);
				}
				index++;
			}
		}
		// If we have deleted the newly added alias, ensure we select one
		if (defaultAlias.getSelectedValue() == null) {
			defaultAlias.setSelectedIndex(0);
		}
	}

	public static void registerType() {
		GwtWidgetElement.register(TYPE, w -> new EditDomainGeneralEditor());
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();
		JsDomain jsDomain = map.get(DomainKeys.domain.name()).cast();

		Set<String> domainAliases = new HashSet<>();
		JsArrayString jsAliases = jsDomain.getAliases();
		for (int i = 0; i < jsAliases.length(); i++) {
			domainAliases.add(jsAliases.get(i));
		}
		aliases.setValues(domainAliases);

		Domain domain = new DomainGwtSerDer().deserialize(new JSONObject(jsDomain));
		description.setText(domain.description);
		name.setText(domain.name);

		loadDomains(domain.name);

		String domainLanguage = SettingsModel.domainSettingsFrom(model).get(DomainSettingsKeys.lang.name());
		domainLanguage = null != domainLanguage ? domainLanguage : LocaleIdTranslation.DEFAULT_ID;
		language.setSelectedIndex(Optional.ofNullable(languageMapping.get(domainLanguage))
				.orElseGet(() -> languageMapping.get(LocaleIdTranslation.DEFAULT_ID)));

		domainDateFormat = SettingsModel.domainSettingsFrom(model).get(DomainSettingsKeys.date.name());
		domainDateFormat = domainDateFormat != null ? domainDateFormat : DateFormatTranslation.DEFAULT_DATE_FORMAT;
		Integer index = dateFormatMapping.get(domainDateFormat);
		if (index == null) {
			dateFormat.addItem(DateFormatTranslation.prettyDateFormatToDisplay(domainDateFormat));
			index = dateFormat.getItemCount() - 1;
		}
		dateFormat.setSelectedIndex(index);

		String domainTimeFormat = SettingsModel.domainSettingsFrom(model).get(DomainSettingsKeys.timeformat.name());
		domainTimeFormat = domainTimeFormat != null ? domainTimeFormat : TimeFormatTranslation.DEFAULT_TIME_FORMAT;
		timeFormat.setSelectedIndex(Optional.ofNullable(timeFormatMapping.get(domainTimeFormat))
				.orElseGet(() -> timeFormatMapping.get(TimeFormatTranslation.DEFAULT_TIME_FORMAT)));

		String domainTz = SettingsModel.domainSettingsFrom(model).get(DomainSettingsKeys.timezone.name());
		domainTz = null != domainTz ? domainTz : DEFAULT_TZ;
		tz.setSelectedIndex(Optional.ofNullable(tzMapping.get(domainTz)).orElseGet(() -> tzMapping.get(DEFAULT_TZ)));

		loadCompositionFont(model);

		String externalUrlSetting = SettingsModel.domainSettingsFrom(model).get(DomainSettingsKeys.external_url.name());
		if (null != externalUrlSetting) {
			externalUrl.setText(externalUrlSetting);
		}
		externalUrl.setReadOnly(!Ajax.TOKEN.isDomainGlobal());
		externalUrl.setEnabled(!externalUrl.isReadOnly());

		String otherUrlsSetting = SettingsModel.domainSettingsFrom(model).get(DomainSettingsKeys.other_urls.name());
		if (null != otherUrlsSetting) {
			otherUrls.setText(otherUrlsSetting);
		}
		otherUrls.setReadOnly(!Ajax.TOKEN.isDomainGlobal());
		otherUrls.setEnabled(!otherUrls.isReadOnly());

		String defaultDomainSetting = SettingsModel.domainSettingsFrom(model)
				.get(DomainSettingsKeys.default_domain.name());
		if (null == defaultDomainSetting) {
			defaultDomainSetting = SettingsModel.domainSettingsFrom(model).get(SysConfKeys.default_domain.name());
		}
		domainList.setSelectedIndex(SysConfAuthenticationEditor.detectDomainIndex(domainList, defaultDomainSetting));

		setAvailableDefaultAliases();
		if (defaultAliasesMapping.containsKey(domain.defaultAlias)) {
			defaultAlias.setSelectedIndex(defaultAliasesMapping.get(domain.defaultAlias));
		} else {
			defaultAlias.setSelectedIndex(0);
		}
		aliases.setReadOnly(defaultAlias.getSelectedValue(), true);
	}

	private void loadCompositionFont(JavaScriptObject model) {
		String compositionSetting = SettingsModel.domainSettingsFrom(model)
				.get(DomainSettingsKeys.domain_composer_font_stack.name());

		if (compositionSetting == null || compositionSetting.isEmpty()) {
			fallbackFonts.setSelectedIndex(fallbackFontMapping.get(DEFAULT_FALLBACK_FONT));
			return;
		}

		String[] fontList = compositionSetting.split(";");
		// keep only the first one
		String[] compositionSplit = fontList[0].split(",");
		String fallback = compositionSplit[compositionSplit.length - 1];
		String font = fontList[0].replace("," + fallback, "");
		Optional<Integer> ofNullable = Optional.ofNullable(fallbackFontMapping.get(fallback));
		if (ofNullable.isPresent()) {
			compositionFont.setText(font);
			fallbackFonts.setSelectedIndex(ofNullable.get());
		} else {
			compositionFont.setText(compositionSetting);
			fallbackFonts.setSelectedIndex(fallbackFontMapping.get(DEFAULT_FALLBACK_FONT));
		}
	}

	private String saveCompositionFont() {
		String font = compositionFont.getText();
		String fallback = fallbackFonts.getSelectedItemText();

		StringBuilder b = new StringBuilder();
		if (font != null && !font.isEmpty()) {
			b.append(font);
			b.append(",");
			b.append(fallback);
		}

		return b.toString();
	}

	@Override
	public void saveModel(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();
		JsDomain jsDomain = map.get(DomainKeys.domain.name()).cast();
		jsDomain.setDescription(description.getText());
		map.put(DomainKeys.domain.name(), jsDomain);

		SettingsModel.domainSettingsFrom(model).putString(DomainSettingsKeys.lang.name(),
				LocaleIdTranslation.getIdByLanguage(language.getSelectedItemText()));
		String selectedItemText = dateFormat.getSelectedItemText();
		SettingsModel.domainSettingsFrom(model).putString(DomainSettingsKeys.date.name(),
				DateFormatTranslation.getKeyByFormat(selectedItemText != null ? selectedItemText : domainDateFormat));
		SettingsModel.domainSettingsFrom(model).putString(DomainSettingsKeys.timeformat.name(),
				TimeFormatTranslation.getKeyByFormat(timeFormat.getSelectedItemText()));

		SettingsModel.domainSettingsFrom(model).putString(DomainSettingsKeys.timezone.name(), tz.getSelectedItemText());

		SettingsModel.domainSettingsFrom(model).putString(DomainSettingsKeys.domain_composer_font_stack.name(),
				saveCompositionFont());

		SettingsModel.domainSettingsFrom(model).putString(DomainSettingsKeys.other_urls.name(), otherUrls.getText());
		SettingsModel.domainSettingsFrom(model).putString(DomainSettingsKeys.external_url.name(),
				externalUrl.getText());
		if (externalUrl.getText().isEmpty()) {
			SettingsModel.domainSettingsFrom(model).putString(DomainSettingsKeys.ssl_certif_engine.name(),
					CertificateDomainEngine.DISABLED.name());
		}

		SettingsModel.domainSettingsFrom(model).putString(DomainSettingsKeys.default_domain.name(),
				domainList.getSelectedValue());

		JSONArray updatedAliasValues = new JSONArray();
		int index = 0;
		for (String alias : aliases.getValues()) {
			if (!alias.trim().isEmpty()) {
				updatedAliasValues.set(index++, new JSONString(alias));
			}
		}
		map.put(DomainKeys.aliases.name(), updatedAliasValues.getJavaScriptObject());
		map.put(DomainKeys.defaultAlias.name(), defaultAlias.getSelectedValue());
		map.put(DomainKeys.defaultDomain.name(), domainList.getSelectedValue());
	}

	private void loadDomains(String domainUid) {
		ItemValue<Domain> domain = DomainsHolder.get().getDomainByUid(domainUid);
		domainList.addItem("---", "");
		SysConfAuthenticationEditor.expandDomainAlias(domainList, Arrays.asList(domain));
	}

}
