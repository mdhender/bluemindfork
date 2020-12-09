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
package net.bluemind.ui.adminconsole.system.systemconf.auth;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsonUtils;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.ui.adminconsole.base.DomainsHolder;
import net.bluemind.ui.adminconsole.system.systemconf.SysConfModel;
import net.bluemind.ui.adminconsole.system.systemconf.auth.l10n.SysConfAuthConstants;
import net.bluemind.ui.common.client.forms.StringEdit;
import net.bluemind.ui.common.client.forms.TrPanel;

public class SysConfAuthenticationEditor extends CompositeGwtWidgetElement {

	public static final String TYPE = "bm.ac.SysConfAuthenticationEditor";
	@UiField
	TrPanel choicePanel;

	@UiField
	StringEdit casUrl;

	@UiField
	ListBox casDomain;

	@UiField
	HTMLPanel casAuthParams;

	@UiField
	HTMLPanel krbAuthParams;

	@UiField
	StringEdit krbAdDomain;

	@UiField
	StringEdit krbAdIp;

	@UiField
	FormPanel keyUploadForm;

	@UiField
	FileUpload keyUpload;

	@UiField
	CheckBox keyFilePresent;

	@UiField
	ListBox krbDomain;

	@UiField
	ListBox domainList;

	private ListBox authTypeSel;

	private String keyTab;

	private static SysConfAuthenticationUiBinder uiBinder = GWT.create(SysConfAuthenticationUiBinder.class);

	interface SysConfAuthenticationUiBinder extends UiBinder<HTMLPanel, SysConfAuthenticationEditor> {
	}

	protected SysConfAuthenticationEditor() {
		HTMLPanel panel = uiBinder.createAndBindUi(this);
		initWidget(panel);
		authTypeSel = new ListBox();
		authTypeSel.addItem(SysConfAuthConstants.INST.authInternal());
		authTypeSel.addItem(SysConfAuthConstants.INST.authCAS());
		authTypeSel.addItem(SysConfAuthConstants.INST.authKerberos());
		authTypeSel.addChangeHandler(event -> {
			AuthType at = AuthType.getByIndex(authTypeSel.getSelectedIndex());
			updateAuthType(at, false);
		});
		choicePanel.add(new Label(SysConfAuthConstants.INST.authType()), "label");
		choicePanel.add(authTypeSel);
		loadDomains();
		setupUploadForm();
	}

	public static void registerType() {
		GwtWidgetElement.register(TYPE, we -> new SysConfAuthenticationEditor());
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		SysConfModel map = SysConfModel.from(model);

		if (map.get(SysConfKeys.default_domain.name()) != null) {
			domainList.setSelectedIndex(detectDomainIndex(domainList, map.get(SysConfKeys.default_domain.name())));
		}

		if (null != map.get(SysConfKeys.cas_url.name())) {
			casUrl.setStringValue(map.get(SysConfKeys.cas_url.name()));
		}
		if (null != map.get(SysConfKeys.cas_domain.name())) {
			casDomain.setSelectedIndex(detectDomainIndex(casDomain, map.get(SysConfKeys.cas_domain.name())));
		}
		if (null != map.get(SysConfKeys.krb_ad_domain.name())) {
			krbAdDomain.setStringValue(map.get(SysConfKeys.krb_ad_domain.name()));
		}
		if (null != map.get(SysConfKeys.krb_ad_ip.name())) {
			krbAdIp.setStringValue(map.get(SysConfKeys.krb_ad_ip.name()));
		}
		if (null != map.get(SysConfKeys.krb_domain.name())) {
			krbDomain.setSelectedIndex(detectDomainIndexFromValue(krbDomain, map.get(SysConfKeys.krb_domain.name())));
		}
		if (map.get(SysConfKeys.auth_type.name()) != null) {
			authTypeSel.setSelectedIndex(detectAuthTypeIndex(map.get(SysConfKeys.auth_type.name())));
		} else {
			authTypeSel.setSelectedIndex(detectAuthTypeIndex(null));
		}
		updateAuthType(AuthType.getByIndex(authTypeSel.getSelectedIndex()), false);
	}

	@Override
	public void saveModel(JavaScriptObject model) {
		SysConfModel map = SysConfModel.from(model);

		map.putString(SysConfKeys.default_domain.name(), domainList.getSelectedValue());

		AuthType at = AuthType.getByIndex(authTypeSel.getSelectedIndex());
		map.putString(SysConfKeys.auth_type.name(), at.name());

		ValidationResult kerberosValidation = checkKerberosParameters();
		if (kerberosValidation.valid) {
			map.putString(SysConfKeys.krb_ad_domain.name(), krbAdDomain.getStringValue());
			map.putString(SysConfKeys.krb_ad_ip.name(), krbAdIp.getStringValue());
			map.putString(SysConfKeys.krb_domain.name(), krbDomain.getSelectedValue());
			if (null != keyTab && keyTab.length() > 0) {
				map.putString(SysConfKeys.krb_keytab.name(), keyTab);
			}
		} else {
			if (map.get(SysConfKeys.auth_type.name()).equalsIgnoreCase("kerberos")) {
				throw new RuntimeException(kerberosValidation.message);
			}
		}

		ValidationResult casValidation = checkCASParameters();
		if (casValidation.valid) {
			map.putString(SysConfKeys.cas_url.name(), casUrl.getStringValue());
			map.putString(SysConfKeys.cas_domain.name(), casDomain.getSelectedValue());
		} else {
			if (map.get(SysConfKeys.auth_type.name()).equalsIgnoreCase("cas")) {
				throw new RuntimeException(casValidation.message);
			}
		}

	}

	private ValidationResult checkCASParameters() {
		final String url = casUrl.getStringValue();
		// CasURL should match http(s) URL regex
		if (url == null || !url.matches("^https?://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]/$")) {
			return ValidationResult.inValid("CAS URL should be a valid http(s) url and end with /");
		}
		return ValidationResult.valid();
	}

	private ValidationResult checkKerberosParameters() {
		// krbAdIp should be an IP value
		if (krbAdIp.getStringValue() == null || !krbAdIp.getStringValue()
				.matches("^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
						+ "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$")) {
			return ValidationResult.inValid("Active Directory external IP should be an IP address (ex : 127.0.0.1)");
		} else if (krbAdDomain.getStringValue() == null) {
			return ValidationResult.inValid("Active Directory domain should not be empty");
		}
		return ValidationResult.valid();
	}

	private void updateAuthType(AuthType authType, boolean updateList) {
		switch (authType) {
		case INTERNAL:
			casAuthParams.setVisible(false);
			krbAuthParams.setVisible(false);
			break;
		case CAS:
			casAuthParams.setVisible(true);
			krbAuthParams.setVisible(false);
			break;
		case KERBEROS:
			casAuthParams.setVisible(false);
			krbAuthParams.setVisible(true);
			break;
		}

		if (!updateList)
			return;

		for (int i = 0; i < authTypeSel.getItemCount(); i++) {
			if (authType.name().equals(authTypeSel.getValue(i))) {
				authTypeSel.setSelectedIndex(i);
				break;
			}
		}
	}

	private void loadDomains() {
		List<ItemValue<Domain>> domains = DomainsHolder.get().getDomains();
		domainList.addItem("---", "");

		for (ItemValue<Domain> domain : domains) {
			if (!"global.virt".equals(domain.value.name)) {
				krbDomain.addItem(domain.value.defaultAlias, domain.value.name);

				expandDomainAlias(domainList, domain);
				expandDomainAlias(casDomain, domain);
			}
		}
	}

	private void expandDomainAlias(ListBox domainList, ItemValue<Domain> domain) {
		domainList.addItem(domain.value.name);
		domain.value.aliases.stream().forEach(domainList::addItem);
	}

	private void setupUploadForm() {
		keyFilePresent.setEnabled(false);
		keyUploadForm.setEncoding(FormPanel.ENCODING_MULTIPART);
		keyUploadForm.setMethod(FormPanel.METHOD_POST);
		String uploadUrl = GWT.getModuleBaseURL();
		uploadUrl = uploadUrl.substring(0, uploadUrl.lastIndexOf("/"));
		uploadUrl = uploadUrl.substring(0, uploadUrl.lastIndexOf("/") + 1);
		keyUploadForm.setAction(uploadUrl + "fileupload");
		keyUploadForm.addSubmitCompleteHandler(event -> {
			keyUploadForm.reset();
			String fileData = new InlineHTML(event.getResults()).getText();
			JavaScriptObject safeEval = JsonUtils.safeEval(fileData);
			JSONObject fileUploadData = new JSONObject(safeEval);
			keyTab = fileUploadData.get("data").isString().stringValue();
			keyFilePresent.setValue(true);
		});
		keyUpload.addChangeHandler(evt -> keyUploadForm.submit());
	}

	private int detectDomainIndex(ListBox domainList, String domain) {
		if (null == domain || domain.isEmpty()) {
			return 0;
		}

		for (int i = 0; i < domainList.getItemCount(); i++) {
			if (domainList.getItemText(i).equals(domain)) {
				return i;
			}
		}

		return 0;
	}

	private int detectDomainIndexFromValue(ListBox domainList, String domain) {
		if (null == domain || domain.isEmpty()) {
			return 0;
		}

		for (int i = 0; i < domainList.getItemCount(); i++) {
			if (domainList.getValue(i).equals(domain)) {
				return i;
			}
		}

		return 0;
	}

	private int detectAuthTypeIndex(String authType) {
		if (null == authType || authType.isEmpty()) {
			return AuthType.INTERNAL.ordinal();
		}
		return AuthType.getIndexByName(authType);
	}

	public static native JSONObject getJsObject(String responseText)
	/*-{
		return JSON.parse(responseText);
	}-*/;

	private static class ValidationResult {
		public final String message;
		public final boolean valid;

		private ValidationResult(String message, boolean valid) {
			this.message = message;
			this.valid = valid;
		}

		static ValidationResult valid() {
			return new ValidationResult(null, true);
		}

		static ValidationResult inValid(String message) {
			return new ValidationResult(message, false);
		}
	}

}
