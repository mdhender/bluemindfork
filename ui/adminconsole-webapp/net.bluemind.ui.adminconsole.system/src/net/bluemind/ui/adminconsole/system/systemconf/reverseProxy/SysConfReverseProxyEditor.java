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
package net.bluemind.ui.adminconsole.system.systemconf.reverseProxy;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.IntegerBox;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.TextBox;

import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.ui.adminconsole.system.systemconf.SysConfModel;

public class SysConfReverseProxyEditor extends CompositeGwtWidgetElement {

	public static final String TYPE = "bm.ac.SysConfReverseProxyEditor";
	@UiField
	PasswordTextBox swPassword;
	@UiField
	IntegerBox workerConnections;
	@UiField
	CheckBox httpProxyEnabled;
	@UiField
	TextBox httpProxyHostname;
	@UiField
	TextBox httpProxyPort;
	@UiField
	TextBox httpProxyLogin;
	@UiField
	TextBox httpProxyPassword;
	@UiField
	TextBox httpProxyExceptions;
	@UiField
	TextBox externalUrl;

	private static SysConfReverseProxyUiBinder uiBinder = GWT.create(SysConfReverseProxyUiBinder.class);

	interface SysConfReverseProxyUiBinder extends UiBinder<HTMLPanel, SysConfReverseProxyEditor> {
	}

	protected SysConfReverseProxyEditor() {
		HTMLPanel panel = uiBinder.createAndBindUi(this);
		initWidget(panel);
	}

	public static void registerType() {
		GwtWidgetElement.register(TYPE, new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

			@Override
			public IGwtWidgetElement create(WidgetElement e) {
				return new SysConfReverseProxyEditor();
			}
		});
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		SysConfModel map = SysConfModel.from(model);

		// NGinx
		if (map.get(SysConfKeys.sw_password.name()) != null) {
			swPassword.setText(map.get(SysConfKeys.sw_password.name()).toString());
		}

		if (map.get(SysConfKeys.nginx_worker_connections.name()) != null) {
			int workers = 1024;
			try {
				workers = Integer.parseInt((map.get(SysConfKeys.nginx_worker_connections.name())));
			} catch (NumberFormatException nfe) {
			}

			workerConnections.setValue(workers);
		} else {
			workerConnections.setValue(1024);
		}

		// Http Proxy
		if (Boolean.parseBoolean(map.get(SysConfKeys.http_proxy_enabled.name()))) {
			httpProxyEnabled.setValue(true);
			enableHttpProxyProperties(true);
		} else {
			httpProxyEnabled.setValue(false);
			enableHttpProxyProperties(false);
		}

		httpProxyHostname.setText(map.get(SysConfKeys.http_proxy_hostname.name()));

		String port = map.get(SysConfKeys.http_proxy_port.name());
		httpProxyPort.setText(port == null || port.isEmpty() ? "3128" : port);

		httpProxyLogin.setText(map.get(SysConfKeys.http_proxy_login.name()));
		httpProxyPassword.setText(map.get(SysConfKeys.http_proxy_password.name()));
		httpProxyExceptions.setText(map.get(SysConfKeys.http_proxy_exceptions.name()));

		// External URL
		if (null != map.get(SysConfKeys.external_url.name())) {
			externalUrl.setText(map.get(SysConfKeys.external_url.name()));
		}
	}

	@Override
	public void saveModel(JavaScriptObject model) {
		SysConfModel map = SysConfModel.from(model);

		// NGinx
		map.putString(SysConfKeys.sw_password.name(), swPassword.getText());
		map.putString(SysConfKeys.nginx_worker_connections.name(), workerConnections.getValue().toString());

		// HTTP proxy
		map.putString(SysConfKeys.http_proxy_enabled.name(), httpProxyEnabled.getValue().toString());
		map.putString(SysConfKeys.http_proxy_hostname.name(), httpProxyHostname.getText());
		map.putString(SysConfKeys.http_proxy_port.name(), httpProxyPort.getText());
		map.putString(SysConfKeys.http_proxy_login.name(), httpProxyLogin.getText());
		map.putString(SysConfKeys.http_proxy_password.name(), httpProxyPassword.getText());
		map.putString(SysConfKeys.http_proxy_exceptions.name(), httpProxyExceptions.getText());

		// External URL
		map.putString(SysConfKeys.external_url.name(), externalUrl.getText());
	}

	@UiHandler("httpProxyEnabled")
	void ldapImportChangeHandler(ClickEvent ce) {
		enableHttpProxyProperties(((CheckBox) ce.getSource()).getValue());
	}

	private void enableHttpProxyProperties(boolean enabled) {
		httpProxyHostname.setEnabled(enabled);
		httpProxyPort.setEnabled(enabled);
		httpProxyLogin.setEnabled(enabled);
		httpProxyPassword.setEnabled(enabled);
		httpProxyExceptions.setEnabled(enabled);
	}
}
