package net.bluemind.cli.job;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.scheduledjob.api.IJob;
import net.bluemind.scheduledjob.api.Job;
import net.bluemind.scheduledjob.api.JobQuery;

public class JobCommand {

	private IJob jobsApi;

	public IJob getJobsApi() {
		return jobsApi;
	}

	protected ListResult<Job> getjobs(CliContext ctx, ItemValue<Domain> domain) {
		JobQuery jobQuery = new JobQuery();
		jobsApi = ctx.adminApi().instance(IJob.class);
		if (!domain.uid.equals("global.virt")) {
			jobQuery.domain = domain.uid;
		}
		return jobsApi.searchJob(jobQuery);
	}
}
