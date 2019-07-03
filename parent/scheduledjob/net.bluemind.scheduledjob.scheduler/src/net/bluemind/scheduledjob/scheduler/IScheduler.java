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
package net.bluemind.scheduledjob.scheduler;

import java.util.Date;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.scheduledjob.api.JobExitStatus;

public interface IScheduler {

	/**
	 * Request an execution id to start a job. The task must not do anything if
	 * this method returns null or throws an exception.
	 * 
	 * The exception received here MUST be re-thrown to parent.
	 * 
	 * @param d
	 * @param bj
	 * @param startDate
	 * @return
	 * @throws ServerFault
	 */
	IScheduledJobRunId requestSlot(String domainName, IScheduledJob bj, Date startDate) throws ServerFault;

	void info(IScheduledJobRunId rid, String locale, String logEntry);

	void reportProgress(IScheduledJobRunId rid, int percent);

	/**
	 * Called by jobs at the end of the tick method when no exception occured.
	 * 
	 * @param rid
	 * @param status
	 */
	void finish(IScheduledJobRunId rid, JobExitStatus status);

	void warn(IScheduledJobRunId rid, String locale, String logEntry);

	void error(IScheduledJobRunId rid, String locale, String logEntry);

}
