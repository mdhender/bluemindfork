/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2016
  *
  * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License)
  * or the CeCILL as published by CeCILL.info (version 2 of the License).
  *
  * There are special exceptions to the terms and conditions of the
  * licenses as they are applied to this program. See LICENSE.txt in
  * the directory of this program distribution.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.ui.adminconsole.monitoring.screens;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.Label;

import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.gwtconsoleapp.base.editor.ScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtScreenRoot;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.monitoring.api.gwt.endpoint.MonitoringGwtEndpoint;
import net.bluemind.ui.adminconsole.base.Actions;
import net.bluemind.ui.adminconsole.monitoring.handlers.ServiceInformationHandler;
import net.bluemind.ui.adminconsole.monitoring.util.UIFormatter;
import net.bluemind.ui.adminconsole.monitoring.widgets.ServiceInformationWidget;
import net.bluemind.ui.common.client.forms.Ajax;

public class ServiceInformationScreen extends Composite implements IGwtScreenRoot {

	public static final String TYPE = "bm.ac.ServiceInformationScreen";
	private ScreenRoot screenRoot;

	@UiField
	public ServiceInformationWidget infoWidget;

	@UiField
	public Anchor globalStatusButton;

	@UiField
	public Label execDateLabel;

	@UiHandler("globalStatusButton")
	public void onGlobalStatusButtonClick(ClickEvent event) {
		Actions.get().showWithParams2("checkGlobalStatus", null);
	}

	interface ServiceInformationScreenBinder extends UiBinder<DockLayoutPanel, ServiceInformationScreen> {
	}

	private static ServiceInformationScreenBinder uiBinder = GWT.create(ServiceInformationScreenBinder.class);

	private ServiceInformationScreen(ScreenRoot screenRoot) {
		this.screenRoot = screenRoot;
		DockLayoutPanel dlp = uiBinder.createAndBindUi(this);
		dlp.setHeight("100%");
		initWidget(dlp);
	}

	public static void registerType() {
		GwtScreenRoot.register(TYPE, new IGwtDelegateFactory<IGwtScreenRoot, ScreenRoot>() {

			@Override
			public IGwtScreenRoot create(ScreenRoot screenRoot) {
				return new ServiceInformationScreen(screenRoot);
			}
		});
	}

	@Override
	public void attach(Element parent) {
		parent.appendChild(getElement());
		onAttach();
	}

	@Override
	public void doLoad(final ScreenRoot instance) {
		instance.load(new DefaultAsyncHandler<Void>() {

			@Override
			public void success(Void value) {
				instance.loadModel(instance.getModel());
			}
		});
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		final JsMapStringJsObject map = model.cast();
		String plugin = map.get("plugin").toString();
		String service = map.get("service").toString();
		MonitoringGwtEndpoint endpoint = new MonitoringGwtEndpoint(Ajax.TOKEN.getSessionId());

		endpoint.getServiceInfo(plugin, service, new ServiceInformationHandler(infoWidget));

		this.execDateLabel.getElement().setInnerHTML(UIFormatter.getDateTime());

	}

	@Override
	public void saveModel(JavaScriptObject model) {

	}

}
