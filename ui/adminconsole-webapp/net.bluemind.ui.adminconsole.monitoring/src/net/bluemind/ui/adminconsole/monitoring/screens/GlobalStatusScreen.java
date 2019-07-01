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
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ToggleButton;

import net.bluemind.domain.api.IDomainsAsync;
import net.bluemind.domain.api.gwt.endpoint.DomainsSockJsEndpoint;
import net.bluemind.gwtconsoleapp.base.editor.Ajax;
import net.bluemind.gwtconsoleapp.base.editor.ScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtScreenRoot;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.monitoring.api.Config;
import net.bluemind.monitoring.api.FetchedData;
import net.bluemind.monitoring.api.IMonitoringAsync;
import net.bluemind.monitoring.api.MethodInformation;
import net.bluemind.monitoring.api.ServerInformation;
import net.bluemind.monitoring.api.gwt.endpoint.MonitoringSockJsEndpoint;
import net.bluemind.system.api.IInstallationAsync;
import net.bluemind.system.api.InstallationVersion;
import net.bluemind.system.api.SubscriptionInformations;
import net.bluemind.system.api.gwt.endpoint.InstallationSockJsEndpoint;
import net.bluemind.ui.adminconsole.monitoring.handlers.GeneralInfoHandler;
import net.bluemind.ui.adminconsole.monitoring.handlers.GlobalStatusHandler;
import net.bluemind.ui.adminconsole.monitoring.l10n.ScreensConstants;
import net.bluemind.ui.adminconsole.monitoring.models.Filters;
import net.bluemind.ui.adminconsole.monitoring.util.UIFormatter;
import net.bluemind.ui.adminconsole.monitoring.widgets.MessagesListBoxWidget;

public class GlobalStatusScreen extends Composite implements IGwtScreenRoot {

	public ScreensConstants text;

	public Filters filters;

	public static final String TYPE = "bm.ac.GlobalStatusScreen";
	private ScreenRoot screenRoot;

	@UiField
	public MessagesListBoxWidget listBox;
	@UiField
	public Label cpu;
	@UiField
	public Label ram;
	@UiField
	public Grid disks;
	@UiField
	public Label userCount;
	@UiField
	public Label groupCount;
	@UiField
	public Label phoneCount;
	@UiField
	public Label bmVersion;
	@UiField
	public Label subscriptionExpDate;
	@UiField
	public Label updateDateTime;
	@UiField
	public Button update;
	@UiField
	public ToggleButton okFilter;
	@UiField
	public ToggleButton warningFilter;
	@UiField
	public ToggleButton koFilter;
	@UiField
	public Label webSocketStatus;

	@UiHandler("update")
	public void onUpdateButtonClick(ClickEvent event) {
		this.fetchAllInfo();
	}

	@UiHandler("okFilter")
	public void onOkButtonClick(ClickEvent event) {
		this.filters.ok = this.okFilter.getValue();
		this.listBox.filterView(this.filters);
	}

	@UiHandler("warningFilter")
	public void onWarningButtonClick(ClickEvent event) {
		this.filters.warning = this.warningFilter.getValue();
		this.listBox.filterView(this.filters);
	}

	@UiHandler("koFilter")
	public void onKoButtonClick(ClickEvent event) {
		this.filters.ko = this.koFilter.getValue();
		this.listBox.filterView(this.filters);
	}

	interface GlobalStatusScreenBinder extends UiBinder<DockLayoutPanel, GlobalStatusScreen> {
	}

	private static GlobalStatusScreenBinder uiBinder = GWT.create(GlobalStatusScreenBinder.class);

	private GlobalStatusScreen(ScreenRoot screenRoot) {
		this.screenRoot = screenRoot;
		DockLayoutPanel dlp = uiBinder.createAndBindUi(this);
		dlp.setHeight("100%");
		initWidget(dlp);

		this.text = GWT.create(ScreensConstants.class);

	}

	public static void registerType() {
		GwtScreenRoot.register(TYPE, new IGwtDelegateFactory<IGwtScreenRoot, ScreenRoot>() {

			@Override
			public IGwtScreenRoot create(ScreenRoot screenRoot) {
				return new GlobalStatusScreen(screenRoot);
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
		this.fetchAllInfo();
	}

	@Override
	public void saveModel(JavaScriptObject model) {

	}

	private void fetchAllInfo() {
		checkWebSocket();

		IMonitoringAsync monitoringEndpoint = new MonitoringSockJsEndpoint(Ajax.TOKEN.getSessionId());
		IDomainsAsync domainsEndpoint = new DomainsSockJsEndpoint(Ajax.TOKEN.getSessionId());
		GeneralInfoHandler genInfoHandler = new GeneralInfoHandler(this);
		IInstallationAsync installationEndpoint = new InstallationSockJsEndpoint(Ajax.TOKEN.getSessionId());

		this.updateDateTime.getElement().setInnerHTML(UIFormatter.getDateTime());

		monitoringEndpoint.getPluginsInfo(new GlobalStatusHandler(this));
		installationEndpoint.getSubscriptionInformations(new DefaultAsyncHandler<SubscriptionInformations>() {

			@Override
			public void success(SubscriptionInformations value) {
				subscriptionExpDate.getElement().setInnerHTML(
						text.subscriptionExpDateLabel() + ": " + (value.ends == null ? text.noSubscription()
								: DateTimeFormat.getFormat(PredefinedFormat.DATE_LONG).format(value.ends)));
			}

		});

		installationEndpoint.getVersion(new DefaultAsyncHandler<InstallationVersion>() {

			@Override
			public void success(InstallationVersion value) {
				bmVersion.getElement().setInnerHTML(
						text.bmVersionLabel() + ": " + value.versionName + " (" + value.softwareVersion + ")");
			}
		});

		domainsEndpoint.all(genInfoHandler);

		monitoringEndpoint.getConfig(new DefaultAsyncHandler<Config>() {

			@Override
			public void success(Config value) {
				cpu.getElement().setInnerHTML(value.part.get(0));
				ram.getElement().setInnerHTML(value.part.get(1));
			}
		});

		monitoringEndpoint.getMethodInfo("system", "disks", "usage", new DefaultAsyncHandler<MethodInformation>() {

			@Override
			public void success(MethodInformation value) {
				disks.clear();
				disks.resizeColumns(3);
				int row = 0;
				for (ServerInformation srvInfo : value.serverInfoList) {
					for (FetchedData data : srvInfo.dataList) {
						disks.resizeRows(row + 1);
						disks.setText(row, 0, data.title);
						disks.setText(row, 1, srvInfo.server.name);
						disks.setText(row, 2, data.data);
						row++;
					}
				}
			}
		});

		initFilters();

	}

	private void checkWebSocket() {
		String host = Window.Location.getHost();
		String port = Window.Location.getPort();
		WebSocket wss = WebSocketHelper.create("wss://" + host + ":" + port + "/eventbus");
		wss.onerror = () -> {
			webSocketStatus.setText("NOT OK");
		};

		wss.onopen = () -> {
			webSocketStatus.setText("OK");
		};
	}

	private void initFilters() {
		this.filters = new Filters();

		this.okFilter.setValue(this.filters.ok);
		this.warningFilter.setValue(this.filters.warning);
		this.koFilter.setValue(this.filters.ko);

		this.listBox.filterView(this.filters);
	}

}
