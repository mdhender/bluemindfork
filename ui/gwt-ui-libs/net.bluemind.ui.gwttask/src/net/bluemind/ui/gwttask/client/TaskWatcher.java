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
package net.bluemind.ui.gwttask.client;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.SimplePanel;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.task.api.gwt.endpoint.TaskGwtEndpoint;
import net.bluemind.ui.common.client.forms.Ajax;
import net.bluemind.ui.common.client.forms.ProgressBar;

public class TaskWatcher extends Composite {

	private HTMLPanel dlp;

	@UiField
	SimplePanel taskOutput;

	@UiField
	ProgressBar progress;

	@UiField
	Button seeLogs;

	@UiField
	Button closeTracker;

	private int progressVal;
	private boolean isShowLogs;
	private final int NORMAL_HEIGHT = 320;

	private AsyncHandler<Void> closeHandler;

	private boolean handleCloseEvent;

	private static TaskWatcherUiBinder uib = GWT.create(TaskWatcherUiBinder.class);

	interface TaskWatcherUiBinder extends UiBinder<HTMLPanel, TaskWatcher> {

	}

	public TaskWatcher(boolean showCloseButton) {
		this.dlp = uib.createAndBindUi(this);
		initWidget(dlp);
		dlp.setHeight("100%");
		this.progressVal = 0;
		setProgress(progressVal);
		isShowLogs = false;
		taskOutput.setVisible(false);
		closeTracker.setVisible(showCloseButton);
		this.handleCloseEvent = showCloseButton;

	}

	@UiHandler("closeTracker")
	void closeTracker(ClickEvent ce) {
		closeHandler.success(null);
	}

	@UiHandler("seeLogs")
	void seeLogs(ClickEvent ce) {
		isShowLogs = !isShowLogs;
		taskOutput.setVisible(isShowLogs);
		seeLogs.setText(isShowLogs ? ProgressTexts.INST.hideLogs() : ProgressTexts.INST.showLogs());
		setHeight((isShowLogs ? NORMAL_HEIGHT + "px" : "100%"));
	}

	public void setProgress(int percent) {
		int value = Math.abs(percent);
		value = Math.min(100, percent);
		progress.setProgressPercent(value);
	}

	/**
	 * Add server lines to the output area.
	 * 
	 * Line starting with <code>#progress XX</code> are interpreted as progress
	 * informations. XX is parsed as an integer and used to update the progress
	 * bar width.
	 * 
	 * @param lines
	 */
	public void addOutput(List<String> lines) {
		boolean progressUpdated = false;
		SafeHtmlBuilder html = new SafeHtmlBuilder();
		for (String l : lines) {
			// GWT.log("server: " + l);
			if (l == null) {
				continue;
			}
			if (l.startsWith("#progress ")) {
				progressUpdated = true;
				progressVal = Integer.parseInt(l.substring("#progress ".length()));
			} else {
				html.appendEscaped(l);
				html.appendHtmlConstant("<br/>");
			}
		}
		taskOutput.getElement().setInnerHTML(taskOutput.getElement().getInnerHTML() + html.toSafeHtml().asString());
		scrollToBottom();
		if (!progressUpdated) {
			progressVal++;
		}
		setProgress(progressVal);
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

	private void startTracking(String taskUid, AsyncHandler<Void> asyncHandler) {
		this.closeHandler = asyncHandler;
		TaskGwtEndpoint taskApi = new TaskGwtEndpoint(Ajax.TOKEN.getSessionId(), taskUid);
		ProgressHandler ph = new ProgressHandler(this, taskApi);
		taskApi.status(ph);
	}

	public void setTaskFinished(boolean b, String resultJson) {
		if (!handleCloseEvent) {
			closeHandler.success(null);
		}
	}

	@UiFactory
	ProgressTexts getTexts() {
		return ProgressTexts.INST;
	}

	public static void track(String taskUid) {
		track(taskUid, false);
	}

	public static CompletableFuture<Void> track(String taskUid, boolean autoHide) {

		final DialogBox os = new DialogBox();
		os.addStyleName("dialog");
		// 700, 320
		TaskWatcher ps = new TaskWatcher(!autoHide);

		os.setWidget(ps);
		os.setGlassEnabled(true);
		os.setAutoHideEnabled(true);
		os.setGlassStyleName("modalOverlay");
		os.setModal(true);
		os.setWidth("700px");
		os.setHeight("120px");
		os.center();
		os.show();

		CompletableFuture<Void> ret = new CompletableFuture<>();
		ps.startTracking(taskUid, new AsyncHandler<Void>() {

			@Override
			public void success(Void value) {
				os.hide();
				ret.complete(null);
			}

			@Override
			public void failure(Throwable e) {
				ret.completeExceptionally(e);
			}

		});
		return ret;
	}
}
