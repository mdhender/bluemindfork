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
package net.bluemind.scheduledjob.api;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import net.bluemind.core.api.BMApi;

/**
 * Represents a set of searchable job attributes.
 */
@BMApi(version = "3")
public class JobExecutionQuery {

	/**
	 * Identifying a job by his name (example : "LdapImportJob").
	 */
	public String jobId;

	public String domain;

	/**
	 * Execution ID.
	 */
	public int id;

	/**
	 * Job exit status.
	 */
	public Set<JobExitStatus> statuses;

	/**
	 * To search only active jobs.
	 */
	public boolean active;

	/**
	 * Allow to skip the first result(s).
	 */
	public int from = 0;

	/**
	 * Maximum size for the results.
	 */
	public int size = -1;

	public JobExecutionQuery() {
		statuses = new HashSet<>(EnumSet.allOf(JobExitStatus.class).stream().filter(j -> j != JobExitStatus.UNKNOWN)
				.collect(Collectors.toList()));
	}
}
