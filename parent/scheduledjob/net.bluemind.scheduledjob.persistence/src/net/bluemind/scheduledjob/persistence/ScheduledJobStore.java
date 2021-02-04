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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcAbstractStore;
import net.bluemind.scheduledjob.api.Job;
import net.bluemind.scheduledjob.api.JobExecution;
import net.bluemind.scheduledjob.api.JobExecutionQuery;
import net.bluemind.scheduledjob.api.JobExitStatus;
import net.bluemind.scheduledjob.api.JobPlanification;
import net.bluemind.scheduledjob.api.JobQuery;
import net.bluemind.scheduledjob.api.LogEntry;

public class ScheduledJobStore extends JdbcAbstractStore {

	private static final Logger logger = LoggerFactory.getLogger(ScheduledJobStore.class);
	private static final Creator<Integer> INTEGER_CREATOR = new Creator<Integer>() {
		@Override
		public Integer create(ResultSet con) throws SQLException {
			return con.getInt(1);
		}
	};

	public ScheduledJobStore(DataSource dataSource) {
		super(dataSource);
	}

	public void updateExecution(JobExecution je) throws ServerFault {
		String query = "update t_job_execution set ( " + JobExecutionColumn.cols.names() + ") = ( "
				+ JobExecutionColumn.cols.values() + " ) " + " where id = ?";

		try {
			update(query, je, JobExecutionColumn.statementValues(je));
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	public JobExecution createExecution(JobExecution je) throws ServerFault {
		logger.debug("recording execution for {}", je);

		String query = "insert into t_job_execution (" + JobExecutionColumn.cols.names() + ") values ("
				+ JobExecutionColumn.cols.values() + ")";
		String updateQuery = "update t_job_plan set last_run = ? where domain_uid = ? and job_id = ?";

		Timestamp startDate = je.startDate == null ? null : new Timestamp(je.startDate.getTime());
		Timestamp endDate = je.endDate == null ? null : new Timestamp(je.endDate.getTime());

		return doOrFail(() -> {
			je.id = insertWithSerial(query,
					new Object[] { je.execGroup, je.domainUid, je.jobId, startDate, endDate, je.status.name() });

			update(updateQuery, new Object[] { je.id, je.domainUid, je.jobId });

			return je;
		});

	}

	/**
	 * @param je
	 * @throws ServerFault
	 */
	public void delete(List<Integer> ids) throws ServerFault {
		Object[] params = new Object[] { ids.stream().map(i -> new Long(i)).toArray(Long[]::new) };
		
		try {
			delete("delete from t_job_execution where id = ANY(?)", params);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	/**
	 * @param jobId
	 * @return
	 * @throws SQLException
	 */
	public Job getJobFromId(String jobId) throws SQLException {
		String query = "select job_id, send_report, report_recipients from t_job_plan where job_id = ?";
		return unique(query, JobExecutionColumn.jobCreator(), JobExecutionColumn.jobPopulator(),
				new Object[] { jobId });
	}

	/**
	 * @param d
	 * @param jid
	 */
	public void ensureDefaultPlan(String domainUid, String jid) {
		try {
			String query = "SELECT 1 FROM t_job_plan WHERE domain_uid=? AND job_id=?";
			Integer id = unique(query, INTEGER_CREATOR, new ArrayList<EntityPopulator<Integer>>(0),
					new Object[] { domainUid, jid });

			if (id == null) {
				query = "INSERT INTO t_job_plan (domain_uid, job_id) VALUES (?, ?)";
				insert(query, new Object[] { domainUid, jid });
			}

		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}

	}

	/**
	 * @param jeq
	 * @return
	 * @throws ServerFault
	 */
	public ListResult<JobExecution> findExecutions(JobExecutionQuery jeq) throws ServerFault {
		if (jeq.statuses != null) {
			jeq.statuses = jeq.statuses.stream().filter(j -> j != JobExitStatus.UNKNOWN).collect(Collectors.toSet());
		}

		Object[] params = new Object[] {};
		if (jeq.id <= 0 && jeq.jobId != null) {
			params = new Object[] { jeq.jobId };
		}

		try {
			List<JobExecution> jobs = select(composeQuery(jeq, false), JobExecutionColumn.jobExecutionCreator(),
					JobExecutionColumn.jobExecutionPopulator(), params);

			ListResult<JobExecution> ret = new ListResult<JobExecution>();

			ret.values = jobs;

			if (jeq.size != -1) {
				ret.total = unique(composeQuery(jeq, true), INTEGER_CREATOR, new ArrayList<EntityPopulator<Integer>>(0),
						params);
			} else {
				ret.total = jobs.size();
			}

			return ret;
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	/**
	 * @param jeq
	 * @param counting
	 * @return
	 */
	private String composeQuery(JobExecutionQuery jeq, boolean counting) {
		StringBuilder q = new StringBuilder();
		q.append(" SELECT");
		if (counting) {
			q.append(" COUNT(*)");
		} else {
			q.append(" je.id,");
			q.append(" je.exec_group,");
			q.append(" je.domain_uid,");
			q.append(" je.job_id,");
			q.append(" je.exec_start,");
			q.append(" je.exec_end,");
			q.append(" je.status");
		}
		q.append(" FROM  t_job_execution je");
		q.append(" WHERE 1>0");

		if (jeq.id > 0) {
			q.append(" AND je.id=" + jeq.id);
		}
		// append domain id even if id is given to prevent security issue when
		// loading an id from another domain

		if (jeq.domain != null) {
			q.append(" AND je.domain_uid='").append(jeq.domain).append("'");
		}

		if (jeq.jobId != null) {
			q.append(" AND je.job_id = ?");
		}

		if (jeq.statuses != null && !jeq.statuses.isEmpty()) {
			q.append(" AND je.status IN (");
			boolean comma = false;
			for (JobExitStatus bes : jeq.statuses) {
				if (comma) {
					q.append(", ");
				} else {
					comma = true;
				}
				q.append("'" + bes.name() + "'::t_job_exit_status");
			}
			q.append(" )");
		} else if (jeq.statuses != null) {
			logger.warn("All statuses excluded in query");
			q.append(" AND 0>1 "); // return nothing query
		}

		if (!counting) {
			q.append(" ORDER BY je.exec_start DESC");
			if (jeq.size != -1) {
				q.append(" LIMIT " + jeq.size + " OFFSET " + jeq.from);
			}
		}

		return q.toString();
	}

	/**
	 * @param context
	 * @param jq
	 * @param ret
	 */
	public void loadStatusesAndPlans(SecurityContext context, JobQuery jq, Collection<Job> ret) {
		if (ret == null || ret.isEmpty()) {
			return;
		}

		StringBuilder q = new StringBuilder();
		q.append(" SELECT");
		q.append(" jp.domain_uid,");
		q.append(" jp.job_id,");
		q.append(" jp.kind,");
		q.append(" je.exec_start,");
		q.append(" jp.cron,");
		q.append(" je.status,");
		q.append(" jp.send_report,");
		q.append(" jp.report_recipients");
		q.append(" FROM t_job_plan jp");

		if (jq == null || jq.statuses == null) {
			q.append(" LEFT JOIN t_job_execution je ON jp.last_run=je.id");
		} else {
			q.append(" INNER JOIN t_job_execution je ON jp.last_run=je.id");
		}

		q.append(" WHERE 1>0");
		if (!context.isDomainGlobal()) {
			q.append(" AND jp.domain_uid='").append(context.getContainerUid()).append("'");
		} else if (jq != null && jq.domain != null) {
			q.append(" AND jp.domain_uid='").append(jq.domain).append("'");
		}

		if (jq != null && jq.statuses != null && !jq.statuses.isEmpty()) {
			q.append(" AND je.status IN (");
			boolean comma = false;
			for (JobExitStatus bes : jq.statuses) {
				if (comma) {
					q.append(", ");
				} else {
					comma = true;
				}
				q.append("'" + bes.name() + "'::t_job_exit_status");
			}
			q.append(" )");
		} else if (jq != null && jq.statuses != null && jq.statuses.isEmpty()) {
			logger.warn("All statuses excluded in query");
			return;
		}

		Map<String, Job> idIndex = new HashMap<String, Job>();
		q.append(" AND jp.job_id IN (");
		boolean comma = false;
		for (Job j : ret) {
			idIndex.put(j.id, j);
			if (comma) {
				q.append(",");
			} else {
				comma = true;
			}
			q.append('?');
		}
		q.append(")");

		q.append("ORDER BY jp.job_id, jp.domain_uid");

		try {
			select(q.toString(), JobExecutionColumn.jobCreator(),
					JobExecutionColumn.jobStatusAndPlansPopulator(idIndex),
					ret.stream().map(j -> j.id).toArray(String[]::new));
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}

	}

	/**
	 * @param je
	 * @param entries
	 * @throws ServerFault
	 */
	public void storeLogEntries(int jobExececutionId, Set<LogEntry> entries) throws ServerFault {
		if (jobExececutionId <= 0) {
			throw new ServerFault("In need a job execution id");
		}

		logger.debug("saving {} entries...", entries.size());
		String query = "insert into t_job_log_entry (execution_id, severity, stamp, locale, content) values (?, ?::t_entry_log_level, ?, ?, ?)";

		try {
			batchInsert(query, entries, JobExecutionColumn.logEntryValues(jobExececutionId));
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	/**
	 * @param context
	 * @param execId
	 * @return
	 */
	public Set<LogEntry> fetchLogEntries(SecurityContext context, int execId) {

		StringBuilder q = new StringBuilder();
		q.append(" SELECT");
		q.append(" severity, stamp, locale, content");
		q.append(" FROM t_job_log_entry");

		Object[] params;
		if (!context.isDomainGlobal()) {
			// ensure we can't read log from another domain using an id
			q.append(" INNER JOIN t_job_execution ON t_job_execution.id=execution_id");
			q.append(" WHERE t_job_execution.domain_uid = ?");
			q.append(" AND execution_id = ?");
			params = new Object[] { context.getContainerUid(), execId };
		} else {
			q.append(" WHERE execution_id = ?");
			params = new Object[] { execId };

		}

		// FIXIT-9: limit 20000 to prevent hprof when we saved loads of crap
		q.append(" ORDER BY stamp ASC LIMIT 20000");
		String query = q.toString();

		try {
			return new LinkedHashSet<LogEntry>(select(query, JobExecutionColumn.logEntryCreator(),
					JobExecutionColumn.logEntryPopulator(), params));
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	/**
	 * @param job
	 * @throws ServerFault
	 */
	public void updateJob(Job job) throws ServerFault {
		List<JobPlanification> plans = job.domainPlanification;

		doOrFail(() -> {
			for (JobPlanification jp : plans) {
				ensureDefaultPlan(jp.domain, job.id);
			}

			String query = "UPDATE t_job_plan SET" //
					+ " kind = ?::t_job_plan_kind,"//
					+ " cron = ?,"//
					+ " send_report = ?,"//
					+ " report_recipients = ? "//
					+ " WHERE 1>0"//
					+ " AND job_id = ?"//
					+ " AND domain_uid = ?";
			batchInsert(query, plans, JobExecutionColumn.planValues(job));

			logger.debug("{} plan rows updated.", plans.size());
			return null;
		});
	}

}
