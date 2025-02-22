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
package net.bluemind.ui.adminconsole.directory.user;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HTMLPanel;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.core.task.api.TaskStatus.State;
import net.bluemind.core.task.api.gwt.endpoint.TaskGwtEndpoint;
import net.bluemind.directory.api.MaintenanceOperation;
import net.bluemind.directory.api.RepairConfig;
import net.bluemind.directory.api.gwt.endpoint.DirEntryMaintenanceGwtEndpoint;
import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.ui.common.client.forms.Ajax;
import net.bluemind.ui.gwttask.client.TaskWatcher;

public class UserCheckAndRepair extends CompositeGwtWidgetElement implements IGwtWidgetElement {

	interface GenralUiBinder extends UiBinder<HTMLPanel, UserCheckAndRepair> {
	}

	public static final String TYPE = "bm.ac.UserCheckAndRepair";

	private static GenralUiBinder uiBinder = GWT.create(GenralUiBinder.class);

	private UserCheckAndRepair() {
		HTMLPanel panel = uiBinder.createAndBindUi(this);
		initWidget(panel);
		addIndexMailboxHandler();
	}

	@UiField
	Button checkAndRepair;

	private String userUid;

	private String domainUid;

	private void addIndexMailboxHandler() {
		checkAndRepair.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				String tuid = "repair-" + userUid + "@" + domainUid;

				TaskGwtEndpoint taskService = new TaskGwtEndpoint(Ajax.TOKEN.getSessionId(), tuid);
				taskService.status(new AsyncHandler<TaskStatus>() {
					@Override
					public void success(TaskStatus value) {
						if (null == value || value.state != State.InProgress) {
							execute();
						} else {
							repairSuccess(TaskRef.create(tuid));
						}
					}

					@Override
					public void failure(Throwable e) {
						execute();
					}

					private void execute() {
						DirEntryMaintenanceGwtEndpoint dirEntryMaintenance = new DirEntryMaintenanceGwtEndpoint(
								Ajax.TOKEN.getSessionId(), domainUid, userUid);
						dirEntryMaintenance
								.getAvailableOperations(new DefaultAsyncHandler<List<MaintenanceOperation>>() {
									@Override
									public void success(List<MaintenanceOperation> value) {
										Set<String> opId = value.stream().map(mo -> mo.identifier)
												.collect(Collectors.toSet());

										RepairConfig config = RepairConfig.create(opId, false, true, true);
										dirEntryMaintenance.repair(config, new DefaultAsyncHandler<TaskRef>() {
											@Override
											public void success(TaskRef value) {
												repairSuccess(value);
											}
										});
									}
								});
					}
				});
			}
		});
	}

	private void repairSuccess(TaskRef value) {
		TaskWatcher.track(value.id);
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();
		domainUid = map.getString("domainUid");
		userUid = map.getString("userId");
	}

	public static void registerType() {
		GwtWidgetElement.register(TYPE, new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

			@Override
			public IGwtWidgetElement create(WidgetElement e) {
				return new UserCheckAndRepair();
			}
		});
	}
}
