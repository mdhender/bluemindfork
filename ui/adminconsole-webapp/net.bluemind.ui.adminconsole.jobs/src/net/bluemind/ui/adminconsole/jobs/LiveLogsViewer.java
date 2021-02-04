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
package net.bluemind.ui.adminconsole.jobs;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiHandler;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.gwtconsoleapp.base.editor.ScreenElement;
import net.bluemind.gwtconsoleapp.base.editor.ScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.ScreenRoot.SizeHint;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtScreenRoot;
import net.bluemind.scheduledjob.api.JobExecution;
import net.bluemind.scheduledjob.api.JobExitStatus;
import net.bluemind.scheduledjob.api.LogEntry;
import net.bluemind.ui.adminconsole.base.Actions;

public class LiveLogsViewer extends JobLogsViewer {

	public static final String TYPE = "bm.ac.LiveLogsViewer";
	private int offset;
	private boolean closed;

	public LiveLogsViewer(ScreenRoot root) {
		super(root);
		this.offset = 0;
		this.closed = false;
		progress.setVisible(true);
	}

	@Override
	protected void onScreenShown(Map<String, String> ssr) {

		severityFilter.addListener(this);

		progress.setProgressPercent(0);
		JobExecution je = new JobExecution();
		je.jobId = ssr.get("jobId");
		je.domainUid = ssr.get("domain");
		showLogs(je);
	}

	private void showLogs(final JobExecution liveExec) {
		if (closed) {
			return;
		}

		liveExec.status = JobExitStatus.IN_PROGRESS;

		jobApi.getLogs(liveExec, offset, new AsyncHandler<Set<LogEntry>>() {

			@Override
			public void success(Set<LogEntry> value) {
				logs = new ArrayList<>();
				logs.addAll(value);
				if (!logs.isEmpty()) {
					LogEntry lastEntry = logs.get(logs.size() - 1);
					GWT.log("newOffset is " + offset);
					offset = lastEntry.offset + 1;
				}
				updateUi(logs, severityFilter.getAcceptedStatus(), null);
				scrollToBottom();

				if (!closed) {
					Scheduler.get().scheduleFixedDelay(new RepeatingCommand() {

						@Override
						public boolean execute() {
							showLogs(liveExec);
							return false;
						}
					}, 3000);
				}
			}

			@Override
			public void failure(Throwable e) {

			}
		});

	}

	/**
	 * Scroll to the bottom of this panel.
	 */
	private void scrollToBottom() {
		setScrollPosition(taskOutput.getElement().getPropertyInt("scrollHeight"));
	}

	private void setScrollPosition(int position) {
		taskOutput.getElement().setPropertyInt("scrollTop", position);
	}

	@UiHandler("close")
	void close(ClickEvent ce) {
		// getOverlayScreen().hide(true);
		Actions.get().showWithParams2("jobsManager", MapBuilder.of());
		closed = true;
	}

	public static void registerType() {
		GwtScreenRoot.register(TYPE, new IGwtDelegateFactory<IGwtScreenRoot, ScreenRoot>() {

			@Override
			public IGwtScreenRoot create(ScreenRoot screenRoot) {
				return new LiveLogsViewer(screenRoot);
			}
		});
	}

	public static ScreenElement screenModel() {
		ScreenRoot screenRoot = ScreenRoot.create("liveLogsViewer", TYPE).cast();
		screenRoot.setOverlay(true);
		screenRoot.setSizeHint(SizeHint.create(700, 320));
		return screenRoot;
	}

}
