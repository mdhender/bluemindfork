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
package net.bluemind.ui.adminconsole.system.maintenance;

import java.util.HashMap;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;

import net.bluemind.core.task.api.TaskRef;
import net.bluemind.gwtconsoleapp.base.editor.Ajax;
import net.bluemind.gwtconsoleapp.base.editor.ScreenElement;
import net.bluemind.gwtconsoleapp.base.editor.ScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtScreenRoot;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.system.api.gwt.endpoint.MailDeliveryMgmtGwtEndpoint;
import net.bluemind.ui.adminconsole.base.Actions;

public class MaintenanceScreen extends Composite implements IGwtScreenRoot {

	private static final String TYPE = "bm.ac.MaintenanceScreen";

	private ScreenRoot screenRoot;

	interface MaintenanceBinder extends UiBinder<HTMLPanel, MaintenanceScreen> {
	}

	@UiField
	Button reconstructRoutingTable;

	private static MaintenanceBinder uiBinder = GWT.create(MaintenanceBinder.class);

	private MaintenanceScreen(ScreenRoot screenRoot) {
		this.screenRoot = screenRoot;
		HTMLPanel dlp = uiBinder.createAndBindUi(this);
		dlp.setHeight("100%");
		initWidget(dlp);
		addButtonAction();
	}

	public static void registerType() {
		GwtScreenRoot.register(TYPE, new IGwtDelegateFactory<IGwtScreenRoot, ScreenRoot>() {

			@Override
			public IGwtScreenRoot create(ScreenRoot screenRoot) {
				return new MaintenanceScreen(screenRoot);
			}
		});
	}

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

	private void addButtonAction() {
		reconstructRoutingTable.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				MailDeliveryMgmtGwtEndpoint service = new MailDeliveryMgmtGwtEndpoint(Ajax.TOKEN.getSessionId());
				service.repair(new DefaultAsyncHandler<TaskRef>() {

					@Override
					public void success(TaskRef value) {
						HashMap<String, String> ssr = new HashMap<>();
						ssr.put("task", value.id + "");
						ssr.put("pictures", null);
						ssr.put("return", "system");
						ssr.put("success", "system");
						Actions.get().showWithParams2("progress", ssr);
					}
				});
			}
		});
	}

	@Override
	public void loadModel(JavaScriptObject model) {
	}

	@Override
	public void saveModel(JavaScriptObject model) {
	}

	public static ScreenElement screenModel() {
		return ScreenRoot.create("emailMaintenance", TYPE);
	}

}
