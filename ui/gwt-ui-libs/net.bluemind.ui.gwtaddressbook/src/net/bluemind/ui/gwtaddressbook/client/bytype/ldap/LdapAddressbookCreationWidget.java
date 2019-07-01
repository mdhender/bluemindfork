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
package net.bluemind.ui.gwtaddressbook.client.bytype.ldap;

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

public class LdapAddressbookCreationWidget extends CompositeGwtWidgetElement {

	public static final String TYPE = "bm.addressbook.LdapAddressbookCreation";

	private static LdapAddressbookCreationWidgetUiBinder uiBinder = GWT
			.create(LdapAddressbookCreationWidgetUiBinder.class);

	interface LdapAddressbookCreationWidgetUiBinder extends UiBinder<HTMLPanel, LdapAddressbookCreationWidget> {
	}

	@UiField
	TextBox label;

	@UiField
	TextBox ldapHostname;

	@UiField
	ListBox ldapProtocol;

	@UiField
	TextBox ldapBaseDn;

	@UiField
	TextBox ldapLoginDn;

	@UiField
	TextBox ldapLoginPw;

	@UiField
	TextBox ldapUserFilter;

	@UiField
	TextBox entryUUID;

	@UiField
	Button ldapConnTest;

	@UiField
	Label errorLabel;

	@UiHandler("ldapConnTest")
	void ldapConnTestClickHandler(ClickEvent ce) {
		final ProgressDialogPanel progress = new ProgressDialogPanel();
		progress.setText("Testing...");
		progress.center();
		progress.show();

		LdapParameters params = LdapParameters.create(LdapParameters.DirectoryType.ldap, ldapHostname.getValue(),
				getLdapProtocol(), getAllCertificate(), ldapBaseDn.getValue(), ldapLoginDn.getValue(),
				ldapLoginPw.getValue(), ldapUserFilter.getValue(), entryUUID.getValue());

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
			errorLabel.setText(LdapAddressbookConstants.INST.errorInvalidHostname());
		} else if (value.errorCode == LdapAddressBookErrorCode.INVALID_LDAP_CREDENTIAL) {
			errorLabel.setText(LdapAddressbookConstants.INST.errorInvalidCredential());
		} else if (value.errorCode == LdapAddressBookErrorCode.INVALID_LDAP_BASEDN) {
			errorLabel.setText(LdapAddressbookConstants.INST.errorInvalidDn());
		} else {
			errorLabel.setText(value.errorMsg);
		}

	}

	private boolean getAllCertificate() {
		if (ldapProtocol.getSelectedValue() != null && ldapProtocol.getSelectedValue().endsWith("AllCert")) {
			return true;
		}

		return false;
	}

	public LdapAddressbookCreationWidget() {
		initWidget(uiBinder.createAndBindUi(this));
		ldapUserFilter.setValue("(objectClass=inetOrgPerson)");
		entryUUID.setValue("entryUUID");
	}

	private String getLdapProtocol() {
		return ldapProtocol.getValue(ldapProtocol.getSelectedIndex());
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
		descriptor.getSettings().put("type", LdapParameters.DirectoryType.ldap.name());
		descriptor.getSettings().put("hostname", ldapHostname.getValue());
		descriptor.getSettings().put("protocol", getLdapProtocol());
		descriptor.getSettings().put("allCerticate", getAllCertificate() ? "true" : "false");
		descriptor.getSettings().put("baseDn", ldapBaseDn.getValue());
		descriptor.getSettings().put("loginDn", ldapLoginDn.getValue());
		descriptor.getSettings().put("loginPw", ldapLoginPw.getValue());
		descriptor.getSettings().put("filter", ldapUserFilter.getValue());
		descriptor.getSettings().put("entryUUID", entryUUID.getValue());
		descriptor.getSettings().put("readonly", "true");
	}

	public static void registerType() {

		GwtWidgetElement.register(TYPE, new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

			@Override
			public IGwtWidgetElement create(WidgetElement el) {
				return new LdapAddressbookCreationWidget();
			}
		});
	}
}