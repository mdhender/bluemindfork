/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.cli.job;

import java.util.Optional;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.utils.CliUtils;
import net.bluemind.core.api.ListResult;
import net.bluemind.scheduledjob.api.Job;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "cancel", description = "Cancel a job on global.virt or domain.tld")
public class JobCancelCommand extends JobCommand implements ICmdLet, Runnable {

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("job");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return JobCancelCommand.class;
		}
	}

	@Parameters(paramLabel = "<domain_uid>", description = "global.virt or domain.tld")
	public String target;

	@Option(required = true, names = "--job", description = "Job name")
	public String job;

	protected CliContext ctx;
	protected CliUtils cliUtils;

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		this.cliUtils = new CliUtils(ctx);
		return this;
	}

	@Override
	public void run() {
		ListResult<Job> jobs = super.getjobs(ctx, target);
		runJob(jobs, target);
	}

	private void runJob(ListResult<Job> jobs, String domain) {
		Boolean found = false;
		for (Job entry : jobs.values) {
			if (entry.id.toLowerCase().contains(job.toLowerCase())) {
				ctx.info("Cancelling " + job + " on " + domain);
				found = true;
				this.getJobsApi().cancel(entry.id, domain);
				break;
			}
		}
		if (Boolean.FALSE.equals(found)) {
			ctx.error("Job " + job + " unknown or not found on " + domain);
		}
	}
}
