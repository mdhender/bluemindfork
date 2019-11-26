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
package net.bluemind.scheduledjob.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.quartz.CronExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.jdbc.Columns;
import net.bluemind.scheduledjob.api.Job;
import net.bluemind.scheduledjob.api.JobDomainStatus;
import net.bluemind.scheduledjob.api.JobExecution;
import net.bluemind.scheduledjob.api.JobExitStatus;
import net.bluemind.scheduledjob.api.JobPlanification;
import net.bluemind.scheduledjob.api.JobRec;
import net.bluemind.scheduledjob.api.LogEntry;
import net.bluemind.scheduledjob.api.LogLevel;
import net.bluemind.scheduledjob.api.PlanKind;

public class JobExecutionColumn {

	private static final Logger logger = LoggerFactory.getLogger(JobExecutionColumn.class);

	public static final Columns cols = Columns.create() //
			.col("exec_group") //
			.col("domain_name")//
			.col("job_id")//
			.col("exec_start")//
			.col("exec_end")//
			.col("status", "t_job_exit_status");

	public static ScheduledJobStore.StatementValues<JobExecution> statementValues(JobExecution item) {
		return new ScheduledJobStore.StatementValues<JobExecution>() {

			@Override
			public int setValues(Connection con, PreparedStatement statement, int index, int currentRow,
					JobExecution value) throws SQLException {

				statement.setString(index++, value.execGroup);
				statement.setString(index++, value.domainName);
				statement.setString(index++, value.jobId);
				statement.setTimestamp(index++, new Timestamp(value.startDate.getTime()));
				if (value.endDate != null) {
					statement.setTimestamp(index++, new Timestamp(value.endDate.getTime()));
				} else {
					statement.setNull(index++, Types.TIMESTAMP);
				}
				statement.setString(index++, value.status.name());

				statement.setInt(index++, item.id);
				return index++;
			}

		};

	}

	public static ScheduledJobStore.Creator<JobExecution> jobExecutionCreator() {
		return new ScheduledJobStore.Creator<JobExecution>() {

			@Override
			public JobExecution create(ResultSet con) throws SQLException {
				return new JobExecution();
			}

		};
	}

	public static ScheduledJobStore.EntityPopulator<JobExecution> jobExecutionPopulator() {
		return new ScheduledJobStore.EntityPopulator<JobExecution>() {

			@Override
			public int populate(ResultSet rs, int index, JobExecution value) throws SQLException {

				value.id = rs.getInt(index++);
				value.execGroup = rs.getString(index++);
				value.domainName = rs.getString(index++);
				value.jobId = rs.getString(index++);
				value.startDate = new Date(rs.getTimestamp(index++).getTime());
				Timestamp endDate = rs.getTimestamp(index++);
				if (endDate != null) {
					value.endDate = new Date(endDate.getTime());
				}
				value.status = JobExitStatus.valueOf(rs.getString(index++));

				return index;
			}

		};
	}

	public static ScheduledJobStore.Creator<Job> jobCreator() {
		return new ScheduledJobStore.Creator<Job>() {

			@Override
			public Job create(ResultSet con) throws SQLException {
				return new Job();
			}

		};
	}

	public static ScheduledJobStore.EntityPopulator<Job> jobPopulator() {
		return new ScheduledJobStore.EntityPopulator<Job>() {

			@Override
			public int populate(ResultSet rs, int index, Job value) throws SQLException {

				value.id = rs.getString(index++);
				value.sendReport = rs.getBoolean(index++);
				String recipients = rs.getString(index++);
				if (recipients != null && !recipients.isEmpty()) {
					value.recipients = recipients;
				}

				return index;
			}

		};
	}

	public static ScheduledJobStore.EntityPopulator<Job> jobStatusAndPlansPopulator(Map<String, Job> idIndex) {
		return new ScheduledJobStore.EntityPopulator<Job>() {

			@Override
			public int populate(ResultSet rs, int index, Job value) throws SQLException {

				String domainName = rs.getString(index++);
				String jid = rs.getString(index++);

				value = idIndex.get(jid);

				List<JobDomainStatus> domainStatus = value.domainStatus;
				List<JobPlanification> domainPlanification = value.domainPlanification;
				JobPlanification jp = new JobPlanification();
				jp.kind = PlanKind.valueOf(rs.getString(index++));
				jp.lastRun = rs.getTimestamp(index++);
				jp.domain = domainName;
				String cs = rs.getString(index++);
				if (jp.kind == PlanKind.SCHEDULED) {
					JobRec rec = new JobRec();
					rec.cronString = cs;
					jp.rec = rec;
					if (cs != null) {
						try {
							CronExpression ce = new CronExpression(cs);
							Date nextRun = null;
							if (jp.lastRun != null) {
								logger.debug("lastRun: " + jp.lastRun);
								nextRun = ce.getNextValidTimeAfter(jp.lastRun);
							} else {
								Calendar cal = Calendar.getInstance();
								cal.add(Calendar.MINUTE, -1);
								nextRun = ce.getNextValidTimeAfter(cal.getTime());
							}
							jp.nextRun = nextRun;
						} catch (ParseException pe) {
							logger.error("Invalid cron string: '" + cs + "' (" + pe.getMessage() + ")");
						}
					}
				}
				domainPlanification.add(jp);

				String statusString = rs.getString(index++);
				if (statusString != null) {
					JobDomainStatus ds = new JobDomainStatus();
					ds.domain = domainName;
					ds.status = JobExitStatus.valueOf(statusString);
					domainStatus.add(ds);
				} else {
					logger.warn("No recorded execution in database for " + value.id + "@" + domainName);
				}
				value.sendReport = rs.getBoolean(index++);
				value.recipients = rs.getString(index++);

				return index;
			}

		};
	}

	public static ScheduledJobStore.Creator<LogEntry> logEntryCreator() {
		return new ScheduledJobStore.Creator<LogEntry>() {

			@Override
			public LogEntry create(ResultSet con) throws SQLException {
				return new LogEntry();
			}

		};
	}

	public static ScheduledJobStore.StatementValues<LogEntry> logEntryValues(int jobExececutionId) {

		return (conn, statement, index, currentRow, le) -> {

			statement.setInt(index++, jobExececutionId);
			statement.setString(index++, le.severity.name());
			statement.setTimestamp(index++, new Timestamp(le.timestamp));
			statement.setString(index++, le.locale);
			statement.setString(index++, le.content);

			return index;

		};

	}

	public static ScheduledJobStore.EntityPopulator<LogEntry> logEntryPopulator() {
		return new ScheduledJobStore.EntityPopulator<LogEntry>() {

			@Override
			public int populate(ResultSet rs, int index, LogEntry value) throws SQLException {

				value.severity = LogLevel.valueOf(rs.getString(index++));
				value.timestamp = rs.getTimestamp(index++).getTime();
				value.locale = rs.getString(index++);
				value.content = rs.getString(index++);

				return index;
			}

		};
	}

	public static ScheduledJobStore.StatementValues<JobPlanification> planValues(Job job) {

		return (conn, statement, index, currentRow, jp) -> {

			statement.setString(index++, jp.kind.name());

			if (jp.kind == PlanKind.SCHEDULED) {
				statement.setString(index++, jp.rec.cronString);
			} else {
				statement.setNull(index++, Types.VARCHAR);
			}

			statement.setBoolean(index++, job.sendReport);
			statement.setString(index++, job.recipients);
			statement.setString(index++, job.id);
			statement.setString(index++, jp.domain);

			return index;

		};
	}

}
