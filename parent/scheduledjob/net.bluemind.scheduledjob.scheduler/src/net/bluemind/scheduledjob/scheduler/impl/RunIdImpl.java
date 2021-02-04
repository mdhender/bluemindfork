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
package net.bluemind.scheduledjob.scheduler.impl;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.scheduledjob.api.JobExitStatus;
import net.bluemind.scheduledjob.api.LogEntry;
import net.bluemind.scheduledjob.scheduler.IScheduledJobRunId;

public class RunIdImpl implements IScheduledJobRunId {

	private static final Logger logger = LoggerFactory.getLogger(RunIdImpl.class);

	public String domainUid;
	public String jid;
	public long startTime;
	public long endTime;
	public String groupId;
	public Set<LogEntry> entries;
	public JobExitStatus status;

	public RunIdImpl(String activeGroup, String domainUid, String jid, Date startDate) {
		this.groupId = activeGroup;
		this.domainUid = domainUid;
		this.jid = jid;
		this.startTime = startDate.getTime();
		this.endTime = startTime;
		this.entries = new LinkedHashSet<LogEntry>();
		this.status = JobExitStatus.IN_PROGRESS;
	}

	public void addEntry(LogEntry le) {
		if (entries.size() >= 20000) {
			logger.warn("Not recording '{}' in database, too much logs already.", le.content);
			return;
		}

		entries.add(le);
	}

	public void destroy() {
		entries.clear();
	}

	@Override
	public String toString() {
		return "RunIdImpl [domainUid=" + domainUid + ", jid=" + jid + ", startTime=" + startTime + ", endTime="
				+ endTime + ", groupId=" + groupId + ", status=" + status + "]";
	}

}
