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

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.core.shared.GWT;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.dataprotect.api.GenerationContent;
import net.bluemind.dataprotect.api.RestoreDefinition;
import net.bluemind.dataprotect.api.RestoreOperation;
import net.bluemind.dataprotect.api.gwt.serder.GenerationContentGwtSerDer;
import net.bluemind.ui.adminconsole.base.Actions;
import net.bluemind.ui.adminconsole.dataprotect.l10n.DPTexts;
import net.bluemind.ui.adminconsole.progress.ui.ProgressScreen;

public class RestoreActionHandler extends ActionHandler<ClientRestorable> {

	private final RestoreOperation rop;
	private final RestoreDefinition rd;
	private final GenerationContent gc;

	public RestoreActionHandler(String name, GenerationContent gc, ClientRestorable rest, int generationId,
			RestoreOperation rop) {
		super(name, rest);
		this.rop = rop;
		this.rd = new RestoreDefinition();
		this.gc = gc;
		GWT.log("Restore action " + rop.identifier + " on generation " + generationId);
		rd.generation = generationId;
		rd.item = rest;
		rd.restoreOperationIdenfitier = rop.identifier;
	}

	@Override
	public void handle() {
		DPConfirm dpc = new DPConfirm(getName() + ". " + DPTexts.INST.confirm());
		dpc.setOkCommand(getCommand());
		dpc.center();
	}

	@Override
	public ScheduledCommand getCommand() {
		ScheduledCommand cmd = new ScheduledCommand() {
			@Override
			public void execute() {
				GWT.log("Restore " + getName() + " on " + getObject() + " with " + rop.identifier + " from gen "
						+ rd.generation);

				DpApi.get().run(rd, new AsyncHandler<TaskRef>() {

					@Override
					public void success(TaskRef result) {
						// FIXME
						GWT.log("Should find a way to give " + gc + " value to progress");
						Map<String, String> ssr = new HashMap<>();
						ssr.put("task", result.id + "");
						ssr.put("success", "dpGenBrowser");
						ssr.put("return", "dpGenBrowser");
						GenerationContentGwtSerDer sd = new GenerationContentGwtSerDer();
						String gcString = sd.serialize(gc).toString();
						GWT.log("Passing along:\n" + gcString);
						ssr.put("mode", "restore");
						ProgressScreen.putValue("genContentString", gcString);

						GWT.log("Received restore task to track: " + result.id);
						Actions.get().showWithParams2("progress", ssr);
					}

					@Override
					public void failure(Throwable e) {
						GWT.log(e.getMessage(), e);
					}
				});

			}
		};
		return cmd;
	}

	@Override
	public RestoreOperation getRestoreOp() {
		return rop;
	}

}
