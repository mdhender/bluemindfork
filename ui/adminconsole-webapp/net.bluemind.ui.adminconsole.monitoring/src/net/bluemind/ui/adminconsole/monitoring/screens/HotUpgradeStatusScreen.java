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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TabLayoutPanel;

import net.bluemind.gwtconsoleapp.base.editor.ScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtScreenRoot;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.system.api.hot.upgrade.HotUpgradeProgress;
import net.bluemind.system.api.hot.upgrade.HotUpgradeTask;
import net.bluemind.system.api.hot.upgrade.HotUpgradeTaskFilter;
import net.bluemind.system.api.hot.upgrade.HotUpgradeTaskStatus;
import net.bluemind.system.api.hot.upgrade.IHotUpgradePromise;
import net.bluemind.system.api.hot.upgrade.gwt.endpoint.HotUpgradeGwtEndpoint;
import net.bluemind.ui.adminconsole.monitoring.l10n.ScreensConstants;
import net.bluemind.ui.common.client.forms.Ajax;

public class HotUpgradeStatusScreen extends Composite implements IGwtScreenRoot {

	public ScreensConstants text;
	public static final String TYPE = "bm.ac.HotUpgradeStatusScreen";
	private ScreenRoot screenRoot;
	private final Style s;

	@UiField
	HotUpgradeTasksGrid running;

	@UiField
	HotUpgradeTasksGrid planned;

	@UiField
	HotUpgradeTasksGrid finished;

	@UiField
	Label info;

	@UiField
	TabLayoutPanel tabContainer;

	interface HotUpgradeStatusScreenBinder extends UiBinder<HTMLPanel, HotUpgradeStatusScreen> {
	}

	private static HotUpgradeStatusScreenBinder uiBinder = GWT.create(HotUpgradeStatusScreenBinder.class);

	private static final Resources res = GWT.create(Resources.class);

	public static interface Resources extends ClientBundle {

		@Source("HotUpgradeStatusScreen.css")
		Style editStyle();

	}

	public static interface Style extends CssResource {

	}

	private HotUpgradeStatusScreen(ScreenRoot screenRoot) {
		this.screenRoot = screenRoot;
		s = res.editStyle();
		s.ensureInjected();
		HTMLPanel dlp = uiBinder.createAndBindUi(this);
		dlp.setHeight("100%");
		initWidget(dlp);
		this.text = GWT.create(ScreensConstants.class);
	}

	public static void registerType() {
		GwtScreenRoot.register(TYPE, new IGwtDelegateFactory<IGwtScreenRoot, ScreenRoot>() {

			@Override
			public IGwtScreenRoot create(ScreenRoot screenRoot) {
				return new HotUpgradeStatusScreen(screenRoot);
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

		IHotUpgradePromise service = new HotUpgradeGwtEndpoint(Ajax.TOKEN.getSessionId()).promiseApi();

		List<HotUpgradeTask> runningList = new ArrayList<>();
		List<HotUpgradeTask> plannedList = new ArrayList<>();
		List<HotUpgradeTask> finishedList = new ArrayList<>();

		service.progress().thenAccept(progresses -> {
			if (progresses.isEmpty()) {
				info.setText("No hot upgrade tasks in database (planned, successfull or failed");
			}
			for (HotUpgradeProgress progress : progresses) {
				info.setText(progress.status + ": " + progress.count + " task(s) (last updated at: "
						+ HotUpgradeTasksGrid.sdf.format(progress.lastUpdatedAt) + ")");
			}
		}).thenAccept(vo -> service.running().thenAccept((tasks) -> runningList.addAll(tasks)).thenCompose(v -> {
			return service.list(HotUpgradeTaskFilter.filter(HotUpgradeTaskStatus.PLANNED));
		}).thenCompose(plannedTasks -> {
			plannedList.addAll(plannedTasks);
			return service
					.list(HotUpgradeTaskFilter.filter(HotUpgradeTaskStatus.SUCCESS, HotUpgradeTaskStatus.FAILURE));
		}).thenAccept(finishedTasks -> {
			finishedList.addAll(finishedTasks);
			Collections.sort(finishedList, (a, b) -> Long.compare(b.updatedAt.getTime(), a.updatedAt.getTime()));
		}).thenAccept(v -> {
			tabContainer.addSelectionHandler(event -> {
				switch (event.getSelectedItem()) {
				case 0:
					running.setValues(runningList);
					break;
				case 1:
					planned.setValues(plannedList);
					break;
				case 2:
					finished.setValues(finishedList);
					break;
				}
			});

			tabContainer.selectTab(0);
		}));
	}

	@Override
	public void saveModel(JavaScriptObject model) {

	}

}
