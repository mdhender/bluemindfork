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
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.IntegerBox;
import com.google.gwt.user.client.ui.PasswordTextBox;

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

		if (map.get(SysConfKeys.sw_password.name()) != null) {
			swPassword.setText(map.get(SysConfKeys.sw_password.name()).toString());
		}
		if (map.get(SysConfKeys.nginx_worker_connections.name()) != null) {
			workerConnections.setValue(Integer.parseInt((map.get(SysConfKeys.nginx_worker_connections.name()))));
		} else {
			workerConnections.setValue(1024);
		}
	}

	@Override
	public void saveModel(JavaScriptObject model) {
		SysConfModel map = SysConfModel.from(model);

		map.putString(SysConfKeys.sw_password.name(), swPassword.getText());
		map.putString(SysConfKeys.nginx_worker_connections.name(), workerConnections.getValue().toString());
	}

}
