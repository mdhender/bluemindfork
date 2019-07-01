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
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.i18n.client.TimeZone;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.SimplePanel;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.api.ListResult;
import net.bluemind.gwtconsoleapp.base.editor.ScreenElement;
import net.bluemind.gwtconsoleapp.base.editor.ScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.ScreenRoot.SizeHint;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtScreenRoot;
import net.bluemind.scheduledjob.api.JobExecution;
import net.bluemind.scheduledjob.api.JobExecutionQuery;
import net.bluemind.scheduledjob.api.LogEntry;
import net.bluemind.scheduledjob.api.LogLevel;
import net.bluemind.scheduledjob.api.gwt.endpoint.JobGwtEndpoint;
import net.bluemind.ui.adminconsole.base.Actions;
import net.bluemind.ui.adminconsole.base.ui.ResetReply;
import net.bluemind.ui.common.client.forms.Ajax;
import net.bluemind.ui.common.client.forms.ProgressBar;

public class JobLogsViewer extends Composite implements ISeverityFilterListener, IGwtScreenRoot {

	private static final JobLogsViewerUiBinder uib = GWT.create(JobLogsViewerUiBinder.class);

	interface JobLogsViewerUiBinder extends UiBinder<DockLayoutPanel, JobLogsViewer> {

	}

	private DockLayoutPanel dlp;

	@UiField
	SeverityFilter severityFilter;

	@UiField
	Button close;

	@UiField
	SimplePanel taskOutput;

	@UiField
	ProgressBar progress;

	private String jobId;
	private Integer activeTab;

	List<LogEntry> logs;

	private ScreenRoot instance;

	protected String execId;

	public static final JobGwtEndpoint jobApi = new JobGwtEndpoint(Ajax.TOKEN.getSessionId());

	private static final String TYPE = "bm.ac.JobLogsViewer";

	public JobLogsViewer(ScreenRoot root) {
		this.instance = root;
		this.dlp = uib.createAndBindUi(this);
		initWidget(dlp);
		dlp.setHeight("100%");
	}

	@UiHandler("close")
	void close(ClickEvent ce) {
		// getOverlayScreen().hide(true);
		Actions.get().showWithParams2("editJob", MapBuilder.of("jobId", jobId, "activeTab", "2"));
	}

	protected void onScreenShown(Map<String, String> ssr) {
		this.execId = ssr.get("exec");
		this.jobId = ssr.get("job");
		this.activeTab = Integer.parseInt(ssr.get("activeTab"));
		GWT.log("logs for execution " + execId + " job: " + jobId + " tab: " + activeTab);

		load();
		severityFilter.addListener(this);
	}

	private void load() {
		JobExecutionQuery jeq = new JobExecutionQuery();
		jeq.id = Integer.parseInt(execId);
		jeq.from = 0;
		jeq.size = 1;
		jobApi.searchExecution(jeq, new AsyncHandler<ListResult<JobExecution>>() {

			@Override
			public void success(ListResult<JobExecution> value) {
				JobExecution je = value.values.get(0);
				jobApi.getLogs(je, 0, new AsyncHandler<Set<LogEntry>>() {

					@Override
					public void success(Set<LogEntry> value) {
						updateUi(value, severityFilter.getAcceptedStatus(), null);
					}

					@Override
					public void failure(Throwable e) {
					}
				});

			}

			@Override
			public void failure(Throwable e) {
			}
		});

	}

	protected String locale() {
		String locale = LocaleInfo.getCurrentLocale().getLocaleName().toLowerCase();
		if (locale.length() > 2) {
			locale = locale.substring(0, 2);
		}

		// FIXME
		if ("en".equals(locale) || "fr".equals(locale)) {
			return locale;
		}

		return "en";
	}

	protected void updateUi(Collection<LogEntry> l, Set<LogLevel> status, String filter) {
		this.logs = new ArrayList<LogEntry>(l.size());
		logs.addAll(l);
		String locale = locale();

		SafeHtmlBuilder html = new SafeHtmlBuilder();
		for (LogEntry le : logs) {

			if (null != le.locale && !locale.equalsIgnoreCase(le.locale)) {
				continue;
			}

			if (filter != null && !"".equals(filter.trim()) && !le.content.contains(filter)) {
				GWT.log("grep reject: '" + filter + "' content: " + le.content);
				continue;
			}
			if (!status.contains(le.severity)) {
				GWT.log("reject: " + le.severity + " " + le.content);
				continue;
			}

			if (le.severity == LogLevel.PROGRESS) {
				String pg = le.content;
				try {
					int pct = Integer.parseInt(pg.substring(pg.indexOf(' ') + 1));
					progress.setProgressPercent(pct);
				} catch (Throwable t) {
					GWT.log(t.getMessage(), t);
				}
			}

			html.appendHtmlConstant("<p style=\"" + getStyle(le.severity) + "\">");
			html.appendHtmlConstant(DateTimeFormat.getFormat(PredefinedFormat.DATE_TIME_MEDIUM)
					.format(new Date(le.timestamp), TimeZone.createTimeZone(0)));
			html.appendHtmlConstant(" - ");
			html.appendHtmlConstant(le.severity.name());
			html.appendHtmlConstant(" - ");
			html.appendEscaped(le.content);
			html.appendHtmlConstant("</p>");
		}
		taskOutput.getElement().setInnerHTML(html.toSafeHtml().asString());
	}

	private String getStyle(LogLevel severity) {
		switch (severity) {
		case ERROR:
			return "color: red;";
		case PROGRESS:
			return "color: grey;";
		case WARNING:
			return "color: orange;";
		default:
		case INFO:
			return "color: #666;";
		}
	}

	protected ResetReply reset() {
		severityFilter.removeListener(this);
		// getOverlayScreen().hide(true);
		return ResetReply.OK;
	}

	// public SizeHint getSizeHint() {
	// return new SizeHint(700, 320);
	// }

	@Override
	public void filteredStatusChanged(Set<LogLevel> status, String string) {
		updateUi(logs, status, string);
	}

	@Override
	public void attach(com.google.gwt.dom.client.Element e) {
		DOM.appendChild(e, getElement());
		onScreenShown(MapBuilder.of(instance.getState()));
		onAttach();
	}

	@Override
	public void loadModel(JavaScriptObject model) {
	}

	@Override
	public void saveModel(JavaScriptObject model) {
	}

	@Override
	public void doLoad(ScreenRoot instance) {
	}

	public static void registerType() {
		GwtScreenRoot.register(TYPE, new IGwtDelegateFactory<IGwtScreenRoot, ScreenRoot>() {

			@Override
			public IGwtScreenRoot create(ScreenRoot screenRoot) {
				return new JobLogsViewer(screenRoot);
			}
		});
	}

	public static ScreenElement screenModel() {
		ScreenRoot screenRoot = ScreenRoot.create("jobLogsViewer", TYPE).cast();
		screenRoot.setOverlay(true);
		screenRoot.setSizeHint(SizeHint.create(700, 320));
		return screenRoot;
	}

}
