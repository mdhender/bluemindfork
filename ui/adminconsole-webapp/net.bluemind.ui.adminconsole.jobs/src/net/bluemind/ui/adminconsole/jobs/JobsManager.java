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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SelectionChangeEvent.Handler;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.gwtconsoleapp.base.editor.ScreenElement;
import net.bluemind.gwtconsoleapp.base.editor.ScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtScreenRoot;
import net.bluemind.scheduledjob.api.Job;
import net.bluemind.scheduledjob.api.JobDomainStatus;
import net.bluemind.scheduledjob.api.JobExecution;
import net.bluemind.scheduledjob.api.JobExitStatus;
import net.bluemind.scheduledjob.api.JobKind;
import net.bluemind.scheduledjob.api.JobPlanification;
import net.bluemind.scheduledjob.api.JobQuery;
import net.bluemind.scheduledjob.api.PlanKind;
import net.bluemind.scheduledjob.api.gwt.endpoint.JobGwtEndpoint;
import net.bluemind.ui.adminconsole.base.DomainsHolder;
import net.bluemind.ui.adminconsole.base.ui.ResetReply;
import net.bluemind.ui.adminconsole.base.ui.ScreenShowRequest;
import net.bluemind.ui.common.client.forms.Ajax;

public class JobsManager extends Composite implements IStatusFilterListener, IGwtScreenRoot {

	public static final String TYPE = "bm.ac.JobsManager";
	private static JobsManagerUiBinder uib = GWT.create(JobsManagerUiBinder.class);

	interface JobsManagerUiBinder extends UiBinder<DockLayoutPanel, JobsManager> {

	}

	private DockLayoutPanel dlp;

	@UiField
	StatusFilter statusFilter;

	@UiField
	JobsGrid grid;

	@UiField
	Button startJobs;

	private String filter;

	private HandlerRegistration selHandlerReg;

	private static final JobGwtEndpoint jobApi = new JobGwtEndpoint(Ajax.TOKEN.getSessionId());

	public JobsManager(ScreenRoot screenRoot) {
		this.dlp = uib.createAndBindUi(this);
		initWidget(dlp);
		dlp.setHeight("100%");
	}

	@UiHandler("startJobs")
	void clicked(ClickEvent ce) {
		startJobs.setEnabled(false);
		Collection<Job> jobs = grid.getSelected();

		final Counter counter = new Counter(jobs.size());
		AsyncHandler<Void> ah = new AsyncHandler<Void>() {

			@Override
			public void success(Void value) {
				counter.value--;
				if (counter.value == 0) {
					find(false);
				}
			}

			@Override
			public void failure(Throwable e) {
				GWT.log(e.getMessage(), e);
				success(null);
			}
		};
		for (Job job : jobs) {
			GWT.log("Starting job " + job.id);
			jobApi.start(job.id, job.domainPlanification.iterator().next().domain, ah);
		}

	}

	protected void onScreenShown(ScreenShowRequest ssr) {
		Handler handler = new Handler() {

			@Override
			public void onSelectionChange(SelectionChangeEvent event) {
				Collection<Job> jobs = grid.getSelected();
				startJobs.setEnabled(!jobs.isEmpty());
				Scheduler.get().scheduleDeferred(new ScheduledCommand() {
					@Override
					public void execute() {
						markActiveJobs(grid.getValues(), grid.getActive());
					}
				});
			}
		};
		this.selHandlerReg = grid.addSelectionChangeHandler(handler);

		find(true);
		// AdminCtrl.get().registerDomainChangedListener(this);
		statusFilter.addListener(this);
		addJobExecutionListener();
	}

	private void addJobExecutionListener() {
		Timer t = new Timer() {
			@Override
			public void run() {
				checkJobExecutions();
			}
		};
		t.schedule(3000);
	}

	private void checkJobExecutions() {
		Collection<Job> jobs = grid.getSelected();
		find(false);
		grid.setSelected(jobs);
		if (Window.Location.getHref().contains("jobsManager")) {
			addJobExecutionListener();
		}
	}

	private void find(final boolean firstTime) {
		JobQuery jq = new JobQuery();
		final ItemValue<Domain> domain = DomainsHolder.get().getSelectedDomain();
		if (domain.value.global) {
			jq.domain = null;
		} else {
			jq.domain = domain.uid;
		}
		jq.statuses = statusFilter.getAcceptedStatus();

		jobApi.searchJob(jq, new AsyncHandler<ListResult<Job>>() {

			@Override
			public void success(ListResult<Job> result) {
				List<Job> expanded = expandRows(result.values, filter);
				grid.setValues(expanded);
				ArrayList<JobExecution> activeJobs = new ArrayList<JobExecution>();
				for (Job job : result.values) {
					if (null != job.domainStatus) {
						for (JobDomainStatus status : job.domainStatus) {
							if (domain.value.global || domain.uid.equals(status.domain)) {
								if (status.status == JobExitStatus.IN_PROGRESS) {
									JobExecution exec = new JobExecution();
									exec.domainName = status.domain;
									exec.jobId = job.id;
									activeJobs.add(exec);
								}
							}
						}
					}
				}
				grid.setActiveJobs(activeJobs);
				markActiveJobs(expanded, activeJobs);

				if (firstTime) {
					grid.selectAll(false);
				}
			}

			@Override
			public void failure(Throwable e) {
				GWT.log(e.getMessage(), e);

			}
		});

	}

	private void markActiveJobs(List<Job> expanded, List<JobExecution> resultList) {
		ProvidesKey<Job> provider = grid.getKeyProvider();
		Map<String, JobExecution> idx = new HashMap<String, JobExecution>();
		for (JobExecution je : resultList) {
			String k = "" + je.domainName + "-" + je.jobId;
			idx.put(k, je);
		}
		int cnt = grid.getRowCount();
		for (int i = 0; i < cnt; i++) {
			Job job = grid.getVisibleItem(i);
			String k = provider.getKey(job).toString();
			boolean running = idx.containsKey(k);
			markActive(i, running);
			activateLogsLink(i, running, idx.get(k));
		}
	}

	private void activateLogsLink(int row, boolean running, JobExecution jobExecution) {
		Element ball = grid.getRowElement(row).getCells().getItem(2).getFirstChildElement().getFirstChildElement();
		if (running) {
			ball.removeClassName("translucent");
		} else {
			ball.addClassName("translucent");
		}
	}

	private void markActive(int row, boolean active) {
		// get the color ball cell
		Element ball = grid.getRowElement(row).getCells().getItem(1).getFirstChildElement().getFirstChildElement();
		if (active) {
			ball.addClassName("blink");
		} else {
			ball.removeClassName("blink");
		}
	}

	private List<Job> expandRows(List<Job> ol, String filter) {
		LinkedList<Job> ret = new LinkedList<Job>();
		for (Job j : ol) {
			String id = JobHelper.getShortId(j).toLowerCase();
			if (filter != null && !"".equals(filter.trim()) && !id.contains(filter.toLowerCase())) {
				GWT.log("grep reject: '" + filter + "' content: " + id);
				continue;
			}

			List<JobPlanification> plans = j.domainPlanification;
			Map<String, JobPlanification> byDomPlans = new HashMap<>();
			List<JobDomainStatus> status = j.domainStatus;
			Map<String, JobDomainStatus> byDomStatus = new HashMap<>();
			Set<String> domains = new HashSet<>();
			for (JobPlanification jp : plans) {
				byDomPlans.put(jp.domain, jp);
				domains.add(jp.domain);
			}
			for (JobDomainStatus jd : status) {
				byDomStatus.put(jd.domain, jd);
				domains.add(jd.domain);
			}

			if (!DomainsHolder.get().getSelectedDomain().value.global) {
				domains.add(DomainsHolder.get().getSelectedDomain().uid);
			}
			Set<JobExitStatus> accepted = statusFilter.getAcceptedStatus();
			for (String d : domains) {
				JobPlanification plan = byDomPlans.get(d);
				JobDomainStatus st = byDomStatus.get(d);
				if (plan != null) {
					Job nj = new Job();
					nj.id = j.id;
					nj.description = j.description;
					nj.kind = j.kind;
					ret.add(nj);
					nj.domainPlanification = Arrays.asList(plan);
					if (st != null) {
						nj.domainStatus = Arrays.asList(st);
					}
				} else if (j.kind != JobKind.GLOBAL && accepted == null) {
					Job nj = new Job();
					nj.id = j.id;
					nj.description = j.description;
					nj.kind = j.kind;
					ret.add(nj);
					plan = new JobPlanification();
					plan.domain = d;
					plan.kind = PlanKind.OPPORTUNISTIC;
					nj.domainPlanification = Arrays.asList(plan);
				}
			}
		}
		return ret;
	}

	protected ResetReply reset() {
		statusFilter.removeListener(this);
		// AdminCtrl.get().removeDomainChangedListener(this);
		selHandlerReg.removeHandler();
		selHandlerReg = null;
		return ResetReply.OK;
	}

	@UiFactory
	JobTexts getTexts() {
		return JobTexts.INST;
	}

	// @Override
	// public void activeDomainChanged(Domain newActiveDomain) {
	// find(false, null);
	// }

	@Override
	public void filteredStatusChanged(Set<JobExitStatus> status, String filter) {
		GWT.log("filtered statuses changed");
		this.filter = filter;
		find(false);
	}

	@Override
	public void attach(Element e) {
		DOM.appendChild(e, getElement());
		onScreenShown(new ScreenShowRequest());
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
				return new JobsManager(screenRoot);
			}
		});
	}

	public static ScreenElement screenModel() {
		ScreenRoot screenRoot = ScreenRoot.create("jobsManager", TYPE).cast();
		return screenRoot;
	}

}
