/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.ui.adminconsole.system.authentication;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsonUtils;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.api.auth.AuthDomainProperties;
import net.bluemind.core.api.auth.AuthTypes;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.domain.api.gwt.endpoint.DomainsGwtEndpoint;
import net.bluemind.domain.api.gwt.js.JsDomain;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.ui.adminconsole.system.SettingsModel;
import net.bluemind.ui.adminconsole.system.authentication.l10n.AuthenticationEditorComponentConstants;
import net.bluemind.ui.common.client.forms.Ajax;
import net.bluemind.ui.common.client.forms.StringEdit;
import net.bluemind.ui.common.client.forms.TrPanel;

public class AuthenticationEditorComponent extends CompositeGwtWidgetElement {
	private static AuthenticationEditorComponentUiBinder uiBinder = GWT
			.create(AuthenticationEditorComponentUiBinder.class);

	interface AuthenticationEditorComponentUiBinder extends UiBinder<HTMLPanel, AuthenticationEditorComponent> {
	}

	private class ExternalUrlHandler implements AsyncHandler<String> {
		private final String adDomain;

		public ExternalUrlHandler(String adDomain) {
			super();
			this.adDomain = adDomain;
		}

		@Override
		public void success(String externalUrl) {
			Optional<String> domainUrl = Optional.ofNullable(externalUrl);

			domainUrl.ifPresent(du -> displayKrbPrincName(du, adDomain));

			if (domainUrl.isEmpty()) {
				disableKrbPrincName();
			}
		}

		@Override
		public void failure(Throwable e) {
			disableKrbPrincName();
		}
	}

	public static final String TYPE = "bm.ac.AuthenticationEditorComponent";

	@UiField
	TrPanel choicePanel;

	@UiField
	StringEdit casUrl;

	@UiField
	HTMLPanel casAuthParams;

	@UiField
	HTMLPanel krbAuthParams;

	@UiField
	StringEdit krbAdDomain;

	@UiField
	StringEdit krbAdIp;

	@UiField
	FormPanel krbAdKeytabUploadForm;

	@UiField
	FileUpload krbAdKeytabFile;

	@UiField
	CheckBox krbAdKeytabFilePresent;

	@UiField
	DivElement krbKtpassPrincNameLabel;

	@UiField
	Label krbKtpassPrincName;

	@UiField
	HTMLPanel externalAuthParams;

	@UiField
	StringEdit openidConfUrl;

	@UiField
	StringEdit openidClientId;

	@UiField
	StringEdit openidClientSecret;

	private ListBox authTypeSel;

	private Optional<String> keytabContent = Optional.empty();

	private String domainUid;
	private Optional<String> domainUrl = Optional.empty();

	private static final AuthenticationEditorComponentConstants constants = GWT
			.create(AuthenticationEditorComponentConstants.class);

	public AuthenticationEditorComponent() {
		HTMLPanel panel = uiBinder.createAndBindUi(this);
		initWidget(panel);

		authTypeSel = new ListBox();
		Stream.of(AuthTypes.values()).forEach(this::fillAuthTypeSel);

		authTypeSel.addChangeHandler(event -> updateAuthType(getByIndex(authTypeSel.getSelectedIndex()), false));
		choicePanel.add(new Label(constants.authType()), "label");
		choicePanel.add(authTypeSel);

		setupKrbForm();
	}

	public static void registerType() {
		GwtWidgetElement.register(TYPE, we -> new AuthenticationEditorComponent());
	}

	private void fillAuthTypeSel(AuthTypes authType) {
		switch (authType) {
		case INTERNAL:
			authTypeSel.addItem(constants.authInternal());
			break;
		case CAS:
			authTypeSel.addItem(constants.authCAS());
			break;
		case KERBEROS:
			authTypeSel.addItem(constants.authKerberos());
			break;
		case OPENID:
			authTypeSel.addItem(constants.authExternal());
			break;
		}
	}

	private void updateAuthType(AuthTypes authType, boolean updateList) {
		switch (authType) {
		case INTERNAL:
			casAuthParams.setVisible(false);
			krbAuthParams.setVisible(false);
			externalAuthParams.setVisible(false);
			break;
		case CAS:
			casAuthParams.setVisible(true);
			krbAuthParams.setVisible(false);
			externalAuthParams.setVisible(false);
			break;
		case KERBEROS:
			casAuthParams.setVisible(false);
			krbAuthParams.setVisible(true);
			externalAuthParams.setVisible(false);
			break;
		case OPENID:
			casAuthParams.setVisible(false);
			krbAuthParams.setVisible(false);
			externalAuthParams.setVisible(true);
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

	private void setupKrbForm() {
		krbAdKeytabUploadForm.setEncoding(FormPanel.ENCODING_MULTIPART);
		krbAdKeytabUploadForm.setMethod(FormPanel.METHOD_POST);
		krbAdKeytabUploadForm.setAction(
				Optional.ofNullable(GWT.getModuleBaseURL()).map(url -> url.substring(0, url.lastIndexOf('/')))
						.map(url -> url.substring(0, url.lastIndexOf('/') + 1)).orElse("/adminconsole/")
						+ "fileupload");
		krbAdKeytabUploadForm.addSubmitCompleteHandler(event -> getKeytabValue(event.getResults()));

		krbAdKeytabFile.addChangeHandler(event -> submitKrbKeytabFile());

		krbAdKeytabFilePresent.addValueChangeHandler(changes -> manageAdKeytabPresence(changes.getValue()));

		krbAdDomain.addValueChangeHandler(event -> manageKrbPrincName());
		krbAdDomain.addKeyUpHandler(event -> uppercaseKrbAdDomain());
	}

	private void uppercaseKrbAdDomain() {
		String current = krbAdDomain.getStringValue();
		if (current != null && !current.isEmpty()) {
			krbAdDomain.setStringValue(krbAdDomain.getStringValue().toUpperCase());
		}
	}

	private void manageKrbPrincName() {
		Optional<String> adDomain = Optional.ofNullable(krbAdDomain.getStringValue()).map(String::trim)
				.filter(s -> !s.isEmpty());

		adDomain.ifPresent(this::displayKrbPrincName);

		if (adDomain.isEmpty()) {
			disableKrbPrincName();
		}
	}

	private void displayKrbPrincName(String adDomain) {
		domainUrl.ifPresent(du -> displayKrbPrincName(du, adDomain));

		if (domainUrl.isEmpty()) {
			new DomainsGwtEndpoint(Ajax.TOKEN.getSessionId()).getExternalUrl(domainUid,
					new ExternalUrlHandler(adDomain));
		}
	}

	private void disableKrbPrincName() {
		krbKtpassPrincNameLabel.getStyle().setDisplay(Display.NONE);
		krbKtpassPrincName.setVisible(false);
		krbKtpassPrincName.setText(null);
	}

	private void displayKrbPrincName(String externalUrl, String adDomain) {
		krbKtpassPrincNameLabel.getStyle().setDisplay(Display.FLEX);
		krbKtpassPrincName.setText("HTTP/" + externalUrl + "@" + adDomain.toUpperCase());
		krbKtpassPrincName.setVisible(true);
	}

	private void submitKrbKeytabFile() {
		if (!krbAdKeytabFile.getFilename().isEmpty()) {
			krbAdKeytabUploadForm.submit();
		}
	}

	private void manageAdKeytabPresence(boolean present) {
		krbAdKeytabFilePresent.setValue(present);
		krbAdKeytabFilePresent.setEnabled(present);

		if (present) {
			return;
		}

		keytabContent = Optional.empty();
		krbAdKeytabUploadForm.reset();
	}

	private void getKeytabValue(String formAsText) {
		JavaScriptObject safeEval = JsonUtils.safeEval(formAsText.replaceAll("<(\"[^\"]*\"|'[^']*'|[^'\">])*>", ""));
		JSONObject fileUploadData = new JSONObject(safeEval);
		keytabContent = Optional.ofNullable(fileUploadData.get("data")).map(JSONValue::isString)
				.map(JSONString::stringValue);

		krbAdKeytabFilePresent.setValue(keytabContent.isPresent());
		krbAdKeytabFilePresent.setEnabled(keytabContent.isPresent());
	}

	public void load(String domainUid, Domain domain, SettingsModel domainSettings) {
		this.domainUid = domainUid;
		domainUrl = Optional.ofNullable(domainSettings.get(DomainSettingsKeys.external_url.name()));

		AuthTypes authType = Arrays.stream(AuthTypes.values())
				.filter(at -> at.name().equals(domain.properties.get(AuthDomainProperties.AUTH_TYPE.name())))
				.findFirst().orElse(AuthTypes.INTERNAL);

		switch (authType) {
		case INTERNAL:
			break;
		case CAS:
			casUrl.setStringValue(domain.properties.get(AuthDomainProperties.CAS_URL.name()));
			break;
		case KERBEROS:
			krbAdDomain.setStringValue(domain.properties.get(AuthDomainProperties.KRB_AD_DOMAIN.name()));
			krbAdIp.setStringValue(domain.properties.get(AuthDomainProperties.KRB_AD_IP.name()));
			keytabContent = Optional.ofNullable(domain.properties.get(AuthDomainProperties.KRB_KEYTAB.name()));
			uppercaseKrbAdDomain();
			manageKrbPrincName();
			manageAdKeytabPresence(keytabContent.isPresent());
			break;
		case OPENID:
			openidConfUrl.setStringValue(domain.properties.get(AuthDomainProperties.OPENID_HOST.name()));
			openidClientId.setStringValue(domain.properties.get(AuthDomainProperties.OPENID_CLIENT_ID.name()));
			openidClientSecret.setStringValue(domain.properties.get(AuthDomainProperties.OPENID_CLIENT_SECRET.name()));
			break;
		}

		authTypeSel.setSelectedIndex(getIndexByName(authType.name()));
		updateAuthType(authType, true);
	}

	public void save(JsDomain domain) {
		AuthTypes authType = getByIndex(authTypeSel.getSelectedIndex());

		// Force internal auth on global.virt
		if (domain.getGlobal()) {
			authType = AuthTypes.INTERNAL;
		}

		domain.getProperties().put(AuthDomainProperties.AUTH_TYPE.name(), authType.name());

		manageCas(domain, authType);
		manageKerberos(domain, authType);
		manageExternal(domain, authType);
	}

	private void manageCas(JsDomain domain, AuthTypes authType) {
		if (authType == AuthTypes.CAS) {
			String url = casUrl.getStringValue();
			if (url == null || !url.matches("^https?://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]/$")) {
				throw new RuntimeException(AuthenticationEditorComponentConstants.INST.casUrlInvalid());
			}
			domain.getProperties().put(AuthDomainProperties.CAS_URL.name(), url);
		} else {
			domain.getProperties().remove(AuthDomainProperties.CAS_URL.name());
		}
	}

	private void manageKerberos(JsDomain domain, AuthTypes authType) {
		if (authType == AuthTypes.KERBEROS) {
			domain.getProperties().put(AuthDomainProperties.KRB_AD_DOMAIN.name(),
					trimNotNullOrBlank(krbAdDomain.getStringValue(),
							AuthenticationEditorComponentConstants.INST.krbAdDomainInvalid()).toUpperCase());
			domain.getProperties().put(AuthDomainProperties.KRB_AD_IP.name(), trimNotNullOrBlank(
					krbAdIp.getStringValue(), AuthenticationEditorComponentConstants.INST.krbAdIpInvalid()));
			domain.getProperties().put(AuthDomainProperties.KRB_KEYTAB.name(), keytabContent.orElseThrow(
					() -> new RuntimeException(AuthenticationEditorComponentConstants.INST.keytabContentInvalid())));
		} else {
			domain.getProperties().remove(AuthDomainProperties.KRB_AD_DOMAIN.name());
			domain.getProperties().remove(AuthDomainProperties.KRB_AD_IP.name());
			domain.getProperties().remove(AuthDomainProperties.KRB_KEYTAB.name());
		}
	}

	private void manageExternal(JsDomain domain, AuthTypes authType) {
		if (authType == AuthTypes.OPENID) {
			domain.getProperties().put(AuthDomainProperties.OPENID_HOST.name(),
					trimNotNullOrBlank(openidConfUrl.getStringValue(),
							AuthenticationEditorComponentConstants.INST.openidConfUrlInvalid()));
			domain.getProperties().put(AuthDomainProperties.OPENID_CLIENT_ID.name(),
					trimNotNullOrBlank(openidClientId.getStringValue(),
							AuthenticationEditorComponentConstants.INST.openidClientIdInvalid()));
			domain.getProperties().put(AuthDomainProperties.OPENID_CLIENT_SECRET.name(),
					trimNotNullOrBlank(openidClientSecret.getStringValue(),
							AuthenticationEditorComponentConstants.INST.openidClientSecretInvalid()));
		} else {
			domain.getProperties().remove(AuthDomainProperties.OPENID_HOST.name());
			domain.getProperties().remove(AuthDomainProperties.OPENID_CLIENT_ID.name());
			domain.getProperties().remove(AuthDomainProperties.OPENID_CLIENT_SECRET.name());
		}
	}

	private String trimNotNullOrBlank(String value, String errorMsg) {
		return Optional.ofNullable(value).map(String::trim).filter(s -> !s.isEmpty())
				.orElseThrow(() -> new RuntimeException(errorMsg));
	}

	private AuthTypes getByIndex(int index) {
		for (AuthTypes type : AuthTypes.values()) {
			if (index == type.ordinal()) {
				return type;
			}
		}
		return AuthTypes.INTERNAL;
	}

	private int getIndexByName(String auth) {
		auth = auth.toLowerCase().trim();
		for (AuthTypes type : AuthTypes.values()) {
			if (auth == type.name().toLowerCase()) {
				return type.ordinal();
			}
		}
		return AuthTypes.INTERNAL.ordinal();
	}
}
