/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.ui.gwtaddressbook.client.bytype.ad;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;

import net.bluemind.addressbook.api.gwt.js.JsAddressBookDescriptor;
import net.bluemind.addressbook.ldap.api.ConnectionStatus;
import net.bluemind.addressbook.ldap.api.LdapParameters;
import net.bluemind.addressbook.ldap.api.fault.LdapAddressBookErrorCode;
import net.bluemind.addressbook.ldap.api.gwt.endpoint.LdapAddressBookGwtEndpoint;
import net.bluemind.gwtconsoleapp.base.editor.Ajax;
import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.ProgressDialogPanel;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;

public class AdAddressbookCreationWidget extends CompositeGwtWidgetElement {

	public static final String TYPE = "bm.addressbook.AdAddressbookCreation";

	private static AdAddressbookCreationWidgetUiBinder uiBinder = GWT.create(AdAddressbookCreationWidgetUiBinder.class);

	interface AdAddressbookCreationWidgetUiBinder extends UiBinder<HTMLPanel, AdAddressbookCreationWidget> {
	}

	@UiField
	TextBox label;

	@UiField
	TextBox adHostname;

	@UiField
	ListBox adProtocol;

	@UiField
	TextBox adBaseDn;

	@UiField
	TextBox adLoginDn;

	@UiField
	TextBox adLoginPw;

	@UiField
	TextBox adUserFilter;

	@UiField
	Button adConnTest;

	@UiField
	Label errorLabel;

	@UiHandler("adConnTest")
	void adConnTestClickHandler(ClickEvent ce) {
		final ProgressDialogPanel progress = new ProgressDialogPanel();
		progress.setText("Testing...");
		progress.center();
		progress.show();

		LdapParameters params = LdapParameters.create(LdapParameters.DirectoryType.ad, adHostname.getValue(),
				getLdapProtocol(), getAllCertificate(), adBaseDn.getValue(), adLoginDn.getValue(), adLoginPw.getValue(),
				adUserFilter.getValue(), LdapParameters.AD_UUID);

		new LdapAddressBookGwtEndpoint(Ajax.TOKEN.getSessionId()).testConnection(params,
				new DefaultAsyncHandler<ConnectionStatus>() {

					@Override
					public void success(ConnectionStatus value) {
						progress.hide();

						if (!value.status) {
							showLdapErrorMsg(value);
						} else {
							errorLabel.setText("");
						}

					}

					@Override
					public void failure(Throwable e) {
						progress.hide();
					}
				});
	}

	private void showLdapErrorMsg(ConnectionStatus value) {

		if (value.errorCode == LdapAddressBookErrorCode.INVALID_LDAP_HOSTNAME) {
			errorLabel.setText(AdAddressbookConstants.INST.errorInvalidHostname());
		} else if (value.errorCode == LdapAddressBookErrorCode.INVALID_LDAP_CREDENTIAL) {
			errorLabel.setText(AdAddressbookConstants.INST.errorInvalidCredential());
		} else if (value.errorCode == LdapAddressBookErrorCode.INVALID_LDAP_BASEDN) {
			errorLabel.setText(AdAddressbookConstants.INST.errorInvalidDn());
		} else {
			errorLabel.setText(value.errorMsg);
		}

	}

	private boolean getAllCertificate() {
		if (adProtocol.getSelectedValue() != null && adProtocol.getSelectedValue().endsWith("AllCert")) {
			return true;
		}

		return false;
	}

	public AdAddressbookCreationWidget() {
		initWidget(uiBinder.createAndBindUi(this));
		adUserFilter.setValue("(objectClass=inetOrgPerson)");
	}

	private String getLdapProtocol() {
		return adProtocol.getValue(adProtocol.getSelectedIndex());
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		JsAddressBookDescriptor descriptor = model.cast();
		label.setText(descriptor.getName());
	}

	@Override
	public void saveModel(JavaScriptObject model) {
		JsAddressBookDescriptor descriptor = model.cast();
		descriptor.setName(label.getText());
		descriptor.getSettings().put("type", LdapParameters.DirectoryType.ad.name());
		descriptor.getSettings().put("hostname", adHostname.getValue());
		descriptor.getSettings().put("protocol", getLdapProtocol());
		descriptor.getSettings().put("allCertificate", getAllCertificate() ? "true" : "false");
		descriptor.getSettings().put("baseDn", adBaseDn.getValue());
		descriptor.getSettings().put("loginDn", adLoginDn.getValue());
		descriptor.getSettings().put("loginPw", adLoginPw.getValue());
		descriptor.getSettings().put("filter", adUserFilter.getValue());
		descriptor.getSettings().put("readonly", "true");
	}

	public static void registerType() {

		GwtWidgetElement.register(TYPE, new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

			@Override
			public IGwtWidgetElement create(WidgetElement el) {
				return new AdAddressbookCreationWidget();
			}
		});
	}
}