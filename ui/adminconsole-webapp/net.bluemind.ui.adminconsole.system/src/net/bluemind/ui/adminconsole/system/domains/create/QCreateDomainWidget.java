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
package net.bluemind.ui.adminconsole.system.domains.create;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Visibility;
import com.google.gwt.dom.client.TableElement;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.TextBox;

import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.core.container.model.gwt.js.JsItemValue;
import net.bluemind.gwtconsoleapp.base.editor.JsHelper;
import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;
import net.bluemind.server.api.gwt.js.JsServer;
import net.bluemind.ui.adminconsole.system.hosts.HostKeys;

public class QCreateDomainWidget extends CompositeGwtWidgetElement {

	interface QCreateDomainUiBinder extends UiBinder<HTMLPanel, QCreateDomainWidget> {
	}

	public static final String TYPE = "bm.ac.QCreateDomainWidget";

	private static QCreateDomainUiBinder uiBinder = GWT.create(QCreateDomainUiBinder.class);

	@UiField
	Label errorLabel;

	@UiField
	TextBox name;

	@UiField
	ListBox mailServices;

	@UiField
	TextBox adminLogin;

	@UiField
	PasswordTextBox adminPassword;

	@UiField
	CheckBox createAdmin;

	@UiField
	TableElement createAdminComponent;

	private Map<String, JsItemValue<JsServer>> serverMapping;

	public QCreateDomainWidget() {
		HTMLPanel dlp = uiBinder.createAndBindUi(this);
		initWidget(dlp);
		createAdminComponent.getStyle().setVisibility(Visibility.HIDDEN);
		adminLogin.getElement().setAttribute("autocomplete", "off");
		adminPassword.getElement().setAttribute("autocomplete", "off");
	}

	@UiHandler("createAdmin")
	public void createAdminChanged(ValueChangeEvent<Boolean> valueChangeEvent) {
		createAdminComponent.getStyle().setVisibility(
				valueChangeEvent.getValue() != null && valueChangeEvent.getValue() == true ? Visibility.VISIBLE
						: Visibility.HIDDEN);
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();
		JsArray<JsItemValue<JsServer>> servers = map.get(HostKeys.servers.name()).cast();
		serverMapping = new HashMap<>();
		for (int i = 0; i < servers.length(); i++) {
			JsItemValue<JsServer> server = servers.get(i);
			if (JsHelper.asList(server.getValue().getTags()).contains("mail/imap")) {
				mailServices.addItem(server.getValue().getName(), server.getUid());
				serverMapping.put(server.getUid(), server);
			}
		}
	}

	@Override
	public void saveModel(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();

		QCreateDomainModel mmodel = GWT.create(QCreateDomainModel.class);

		mmodel.name = name.getText();
		mmodel.domainUid = name.getText();
		mmodel.selectedServer = serverMapping.get(mailServices.getSelectedValue());

		Boolean ca = createAdmin.getValue();
		mmodel.createAdmin = ca != null ? ca : false;
		mmodel.adminLogin = adminLogin.getText();
		mmodel.adminPassword = adminPassword.getText();
		map.put("domainModel", mmodel);
	}

	@Override
	public void attach(Element parent) {
		super.attach(parent);
	}

	public static void registerType() {
		GwtWidgetElement.register(TYPE, new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

			@Override
			public IGwtWidgetElement create(WidgetElement e) {
				return new QCreateDomainWidget();
			}
		});
		GWT.log("bm.ac.QCreateDomainWidget registered");

	}

}
