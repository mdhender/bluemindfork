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
package net.bluemind.ui.adminconsole.system.hosts.create;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;

import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;
import net.bluemind.server.api.gwt.js.JsServer;
import net.bluemind.ui.adminconsole.system.hosts.HostKeys;

public class QCreateHostWidget extends CompositeGwtWidgetElement {

	interface QCreateHostUiBinder extends UiBinder<HTMLPanel, QCreateHostWidget> {
	}

	public static final String TYPE = "bm.ac.QCreateHostWidget";

	private static QCreateHostUiBinder uiBinder = GWT.create(QCreateHostUiBinder.class);

	@UiField
	Label errorLabel;

	@UiField
	TextBox name;

	@UiField
	TextBox ip;

	@UiField
	TextBox fqdn;

	public QCreateHostWidget() {
		HTMLPanel dlp = uiBinder.createAndBindUi(this);
		initWidget(dlp);
	}

	@Override
	public void loadModel(JavaScriptObject model) {
	}

	@Override
	public void saveModel(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();
		JsServer server = JsServer.create();
		server.setIp(ip.getText());
		server.setFqdn(fqdn.getText());
		server.setName(name.getText());
		map.put(HostKeys.server.name(), server);
	}

	@Override
	public void attach(Element parent) {
		super.attach(parent);
	}

	public static void registerType() {
		GwtWidgetElement.register(TYPE, new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

			@Override
			public IGwtWidgetElement create(WidgetElement e) {
				return new QCreateHostWidget();
			}
		});
		GWT.log("bm.ac.QCreateHostWidget registered");

	}

}
