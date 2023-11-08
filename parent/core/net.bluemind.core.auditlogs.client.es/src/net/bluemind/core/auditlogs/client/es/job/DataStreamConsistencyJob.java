package net.bluemind.core.auditlogs.client.es.job;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.auditlogs.client.loader.AuditLogLoader;
import net.bluemind.core.auditlogs.exception.DataStreamCreationException;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.api.IDomains;
import net.bluemind.scheduledjob.api.JobExitStatus;
import net.bluemind.scheduledjob.api.JobKind;
import net.bluemind.scheduledjob.scheduler.IScheduledJob;
import net.bluemind.scheduledjob.scheduler.IScheduledJobRunId;
import net.bluemind.scheduledjob.scheduler.IScheduler;

public class DataStreamConsistencyJob implements IScheduledJob {
	private static final Logger logger = LoggerFactory.getLogger(DataStreamConsistencyJob.class);

	public static final String JID = "net.bluemind.core.auditlogs.client.es.job.DataStreamConsistencyJob";

	private static final String AUDIT_LOG_PREFIX = "audit_log";
	private static final String SEPARATOR = "_";

	@Override
	public void tick(IScheduler sched, boolean forced, String domainName, Date startDate) throws ServerFault {

		if (!forced) {
			GregorianCalendar gc = new GregorianCalendar();
			gc.setTime(startDate);
			if (gc.get(Calendar.MINUTE) != 0 || gc.get(Calendar.HOUR_OF_DAY) != 3) {
				return;
			}
		}

		logger.info("Run consistency check for auditlog datastreams");
		IScheduledJobRunId jobRunId = sched.requestSlot(domainName, this, startDate);
		if (jobRunId == null) {
			return;
		}
		IDomains domainsService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomains.class);

		AuditLogLoader auditLogProvider = new AuditLogLoader();
		domainsService.all().forEach(d -> {
			String dataStreamFullName = AUDIT_LOG_PREFIX + SEPARATOR + d.uid;
			boolean isDataStream = auditLogProvider.getManager().isDataStream(dataStreamFullName);
			if (!isDataStream) {
				logger.info("Datastream '{}' does not exist : must be created", dataStreamFullName);
				try {
					auditLogProvider.getManager().createDataStreamForDomainIfNotExists(AUDIT_LOG_PREFIX, d.uid);
				} catch (DataStreamCreationException e) {
					sched.finish(jobRunId, JobExitStatus.FAILURE);
				}
			}
		});

		sched.finish(jobRunId, JobExitStatus.SUCCESS);
	}

	@Override
	public JobKind getType() {
		return JobKind.GLOBAL;
	}

	@Override
	public String getDescription(String locale) {
		if ("fr".equals(locale)) {
			return "Vérifie la cohérence des datastreams ES pour l'auditlog";
		} else {
			return "Checks ES datastream consistency for auditlog";
		}
	}

	@Override
	public String getJobId() {
		return JID;
	}

	@Override
	public Set<String> getLockedResources() {
		return Collections.emptySet();
	}

	@Override
	public boolean supportsScheduling() {
		return true;
	}

}
