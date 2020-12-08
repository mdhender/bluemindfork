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

import java.util.List;
import java.util.Optional;

import com.github.freva.asciitable.AsciiTable;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.utils.CliUtils;
import net.bluemind.core.api.ListResult;
import net.bluemind.scheduledjob.api.Job;
import net.bluemind.scheduledjob.api.JobDomainStatus;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "list", description = "List all runnable jobs.")
public class JobListCommand extends JobCommand implements ICmdLet, Runnable {

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("job");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return JobListCommand.class;
		}
	}

	protected CliContext ctx;
	protected CliUtils cliUtils;

	@Parameters(paramLabel = "<target>", description = "global.virt or domain.tld")
	public String target;

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		this.cliUtils = new CliUtils(ctx);
		return this;
	}

	@Override
	public void run() {
		ListResult<Job> jobs = super.getjobs(ctx, target);
		display(jobs, target);
	}

	private void display(ListResult<Job> jobs, String domain) {
		int size = jobs.values.size();
		String[] headers = { "id", "description", "kind", "status", "sendReport", "recipients" };
		String[][] asTable = new String[size][headers.length];

		int i = 0;
		for (Job entry : jobs.values) {
			asTable[i][0] = entry.id;
			asTable[i][1] = entry.description;
			asTable[i][2] = entry.kind.name();
			asTable[i][3] = getdomainStatus(entry.domainStatus, domain);
			asTable[i][4] = Boolean.toString(entry.sendReport);
			asTable[i][5] = entry.recipients;
			i++;
		}
		ctx.info(AsciiTable.getTable(headers, asTable));
	}

	private String getdomainStatus(List<JobDomainStatus> jobStatus, String domain) {

		for (JobDomainStatus domainStatus : jobStatus) {
			if (domainStatus.domain.equalsIgnoreCase(domain)) {
				return domainStatus.status.toString();
			}
		}
		return null;
	}
}
