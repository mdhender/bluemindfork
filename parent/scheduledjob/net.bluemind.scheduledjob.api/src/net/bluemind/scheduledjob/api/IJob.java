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

import java.util.List;
import java.util.Set;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.fault.ServerFault;

@BMApi(version = "3")
@Path("/scheduledjobs")
public interface IJob {

	/**
	 * List jobs deployed. global & non-global admins will get different results.
	 * 
	 * @param query
	 * @return
	 * @throws ServerFault
	 */
	@POST
	@Path("_searchJob")
	public ListResult<Job> searchJob(JobQuery query) throws ServerFault;

	/**
	 * Fetch recorded job executions. Pagination is supported. global & non-global
	 * admins will get different results.
	 * 
	 * @param query
	 * @return list of job executions, sorted by start execution date in descending
	 *         order
	 * @throws ServerFault
	 */
	@POST
	@Path("_searchExecution")
	public ListResult<JobExecution> searchExecution(JobExecutionQuery query) throws ServerFault;

	/**
	 * Get job using its id
	 * 
	 * @param jobId
	 * @return
	 * @throws ServerFault
	 */
	@GET
	@Path("_job/{jobId}")
	public Job getJobFromId(@PathParam(value = "jobId") String jobId) throws ServerFault;

	/**
	 * Update job planification {@link JobPlanification}
	 * 
	 * @param job
	 * @throws ServerFault
	 */
	@POST
	@Path("_updateJob")
	public void update(Job job) throws ServerFault;

	/**
	 * Removes one execution. Removes nothing if your token has no right on this
	 * execution (global job, different domain, etc)
	 * 
	 * @param execution
	 * @throws ServerFault
	 */
	@DELETE
	@Path("_deleteExecution")
	public void deleteExecution(@QueryParam(value = "jobExecutionId") int jobExecutionId) throws ServerFault;

	/**
	 * Removes multiple executions.
	 * 
	 * @param execution
	 * @throws ServerFault
	 */
	@DELETE
	@Path("_deleteExecutions")
	public void deleteExecutions(List<Integer> executions) throws ServerFault;

	/**
	 * Get active job using its ID
	 * 
	 * force-start a job. Execution is recorded when the job finishes.
	 * 
	 * @param jobId
	 * @throws ServerFault
	 */
	@POST
	@Path("_start/{jobId}")
	public void start(@PathParam(value = "jobId") String jobId, @QueryParam(value = "domainName") String domainName)
			throws ServerFault;

	/**
	 * Cancel running job by its ID
	 * 
	 * @param jobId
	 * @throws ServerFault
	 */
	@DELETE
	@Path("_cancel/{jobId}")
	public void cancel(@PathParam(value = "jobId") String jobId, @QueryParam(value = "domainName") String domainName)
			throws ServerFault;

	/**
	 * Return the logs of a job. The offset indicates how much logs you already
	 * fetched in a previous call.
	 * 
	 * @param execution
	 * @param offset
	 * @return
	 * @throws ServerFault
	 */
	@POST
	@Path("_logs")
	public Set<LogEntry> getLogs(JobExecution jobExecution, @QueryParam(value = "offset") int offset)
			throws ServerFault;
}
