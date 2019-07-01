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
package net.bluemind.ui.adminconsole.dataprotect;

import java.util.concurrent.CompletableFuture;

import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.dataprotect.api.DataProtectGeneration;
import net.bluemind.dataprotect.api.gwt.endpoint.DataProtectGwtEndpoint;
import net.bluemind.ui.adminconsole.base.Actions;
import net.bluemind.ui.adminconsole.base.ui.ScreenShowRequest;
import net.bluemind.ui.adminconsole.dataprotect.l10n.DPTexts;
import net.bluemind.ui.common.client.forms.Ajax;
import net.bluemind.ui.gwttask.client.TaskWatcher;

public class ForgetGenHandler implements ClickHandler {

	private DataProtectGeneration dpg;

	public ForgetGenHandler(DataProtectGeneration dpg) {
		this.dpg = dpg;
	}

	@Override
	public void onClick(ClickEvent event) {
		event.stopPropagation();

		DPConfirm dpc = new DPConfirm(DPTexts.INST.confirmForget());
		dpc.setOkCommand(new ScheduledCommand() {
			@Override
			public void execute() {
				forgetCall();
			}
		});
		dpc.center();
	}

	public void forgetCall() {

		DataProtectGwtEndpoint dpApi = new DataProtectGwtEndpoint(Ajax.TOKEN.getSessionId());
		dpApi.forget(dpg.id, new AsyncHandler<TaskRef>() {

			@Override
			public void success(TaskRef value) {
				CompletableFuture<Void> status = TaskWatcher.track(value.id, false);
				status.thenRun(() -> Actions.get().show("dpNavigator", new ScreenShowRequest()));

			}

			@Override
			public void failure(Throwable e) {
				Actions.get().show("dpNavigator", new ScreenShowRequest());
			}

		});
	}

}
