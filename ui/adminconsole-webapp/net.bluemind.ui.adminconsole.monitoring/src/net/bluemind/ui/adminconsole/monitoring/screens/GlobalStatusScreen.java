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

import java.util.Arrays;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;

import net.bluemind.gwtconsoleapp.base.editor.ScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtScreenRoot;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.metrics.alerts.api.AlertInfo;
import net.bluemind.metrics.alerts.api.AlertLevel;
import net.bluemind.metrics.alerts.api.IMonitoringPromise;
import net.bluemind.metrics.alerts.api.gwt.endpoint.MonitoringGwtEndpoint;
import net.bluemind.ui.adminconsole.monitoring.l10n.ScreensConstants;
import net.bluemind.ui.common.client.forms.Ajax;

public class GlobalStatusScreen extends Composite implements IGwtScreenRoot {

	public ScreensConstants text;
	public static final String TYPE = "bm.ac.GlobalStatusScreen";
	private ScreenRoot screenRoot;
	private final Style s;

	@UiField
	FlexTable alertList;

	@UiField
	ListBox levelSelect;

	@UiField
	Label filterLabel;

	@UiField
	ListBox limitSelect;

	@UiField
	Label limitLabel;

	@UiField
	Label selectLabel;

	@UiField
	CheckBox filterResolved;

	interface GlobalStatusScreenBinder extends UiBinder<HTMLPanel, GlobalStatusScreen> {
	}

	private static GlobalStatusScreenBinder uiBinder = GWT.create(GlobalStatusScreenBinder.class);

	private static final Resources res = GWT.create(Resources.class);

	public static interface Resources extends ClientBundle {

		@Source("GlobalStatusScreen.css")
		Style editStyle();

	}

	public static interface Style extends CssResource {

		String container();

		String header();

		String row();

		String inactive();

		String icon();

		String partnership();

		String action();

		String refreshList();

		String selectList();

	}

	private GlobalStatusScreen(ScreenRoot screenRoot) {
		this.screenRoot = screenRoot;
		s = res.editStyle();
		s.ensureInjected();
		HTMLPanel dlp = uiBinder.createAndBindUi(this);
		dlp.setHeight("100%");
		initWidget(dlp);
		this.text = GWT.create(ScreensConstants.class);

		filterLabel.setText(text.filterResolved());
		selectLabel.setText(text.level());

		levelSelect.addItem(text.all());
		levelSelect.addItem(text.warning());
		levelSelect.addItem(text.critical());

		limitSelect.addItem("30");
		limitSelect.addItem("60");
		limitSelect.addItem("90");
		limitSelect.setStyleName(s.selectList());

		limitLabel.setText(text.days());

		alertList.setStyleName(s.container());
		levelSelect.setStyleName(s.selectList());

		levelSelect.addChangeHandler(e -> refresh());
		limitSelect.addChangeHandler(e -> refresh());
		filterResolved.addValueChangeHandler(e -> refresh());
	}

	private void refresh() {
		int selectedIndex = levelSelect.getSelectedIndex();
		int limitAsInt = Integer.parseInt(limitSelect.getSelectedItemText());
		switch (selectedIndex) {
		case -1:
		case 0:
			loadAlerts(filterResolved.getValue(), limitAsInt,
					Arrays.asList(AlertLevel.OK, AlertLevel.WARNING, AlertLevel.CRITICAL));
			break;
		case 1:
			loadAlerts(filterResolved.getValue(), limitAsInt, Arrays.asList(AlertLevel.WARNING));
			break;
		case 2:
			loadAlerts(filterResolved.getValue(), limitAsInt, Arrays.asList(AlertLevel.CRITICAL));
			break;
		}
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
		filterResolved.setValue(true);
		loadAlerts(true, 30, Arrays.asList(AlertLevel.OK, AlertLevel.WARNING, AlertLevel.CRITICAL));
	}

	private void loadAlerts(boolean filter, int limit, List<AlertLevel> levels) {
		alertList.removeAllRows();
		int idx = 0;
		alertList.setWidget(0, idx++, new Label(text.date()));
		alertList.setWidget(0, idx++, new Label(text.product()));
		alertList.setWidget(0, idx++, new Label(text.id()));
		alertList.setWidget(0, idx++, new Label(text.level()));
		alertList.setWidget(0, idx++, new Label(text.name()));
		alertList.setWidget(0, idx++, new Label(text.host()));
		alertList.setWidget(0, idx++, new Label(text.datalocation()));
		alertList.setWidget(0, idx++, new Label(text.message()));
		alertList.getRowFormatter().setStyleName(0, s.header());

		IMonitoringPromise monitoring = new MonitoringGwtEndpoint(Ajax.TOKEN.getSessionId()).promiseApi();
		monitoring.getAlerts(limit, filter, levels).thenAccept(infos -> {
			for (int i = 0; i < infos.size(); i++) {
				int ridx = 0;
				AlertInfo info = infos.get(i);
				alertList.setWidget(i + 1, ridx++, new Label(info.time.iso8601));
				alertList.setWidget(i + 1, ridx++, new Label(info.product));
				alertList.setWidget(i + 1, ridx++, new Label(info.id));

				Label level = new Label();
				level.setStyleName(alertToStyle(info.level));
				alertList.setWidget(i + 1, ridx++, level);
				alertList.setWidget(i + 1, ridx++, new Label(info.name));
				alertList.setWidget(i + 1, ridx++, new Label(info.host));
				alertList.setWidget(i + 1, ridx++, new Label(info.datalocation));
				alertList.setWidget(i + 1, ridx++, new Label(info.message));
				alertList.getRowFormatter().setStylePrimaryName(i + 1, s.row());
			}
		});
	}

	private String alertToStyle(AlertLevel level) {
		switch (level) {
		case OK:
			return "fa fa-lg fa fa-circle green";
		case WARNING:
			return "fa fa-lg fa-warning";
		case CRITICAL:
			return "fa fa-lg fa fa-circle red";
		default:
			return "fa fa-lg fa fa-circle blue";
		}
	}

	@Override
	public void saveModel(JavaScriptObject model) {

	}

}
