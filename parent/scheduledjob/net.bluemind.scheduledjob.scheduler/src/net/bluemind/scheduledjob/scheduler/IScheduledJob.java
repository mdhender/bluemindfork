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

import java.util.Collections;
import java.util.Date;
import java.util.Set;

import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.scheduledjob.api.JobKind;

public interface IScheduledJob {

	/**
	 * This method is called by Blue Mind's scheduler. When plannedExecution is
	 * true, the task must comply and run (if relevant for the given domain). When
	 * planned execution is false, the task can decide what to do.
	 * 
	 * @param sched
	 * @param forced    True when started by hand on from a scheduled plan. False
	 *                  when the job is in automatic mode.
	 * @param domain
	 * @param startDate
	 */
	void tick(IScheduler sched, boolean forced, String domainName, Date startDate) throws ServerFault;

	JobKind getType();

	String getDescription(String locale);

	String getJobId();

	default Set<String> getLockedResources() {
		return Collections.emptySet();
	}

	boolean supportsScheduling();

	default void cancel() {
		LoggerFactory.getLogger(this.getClass())
				.info("Job {} has been cancelled. The Job provides no cancellation procedure", getJobId());
	}

}
