package net.bluemind.system.importation.commons.scanner;

import java.util.HashMap;
import java.util.Map;

import net.bluemind.scheduledjob.api.JobExitStatus;
import net.bluemind.scheduledjob.scheduler.IScheduledJobRunId;
import net.bluemind.scheduledjob.scheduler.IScheduler;

public class RepportStatus {
	private int errors = 0;
	private int warnings = 0;

	public void error() {
		this.errors++;
	}

	public void warn() {
		this.warnings++;
	}

	public void logStatus(IScheduler sched, IScheduledJobRunId rid) {
		if (sched == null || rid == null) {
			return;
		}

		if (errors != 0) {
			Map<String, String> messages = new HashMap<>();
			messages.put("en", errors + " reported error(s) and " + warnings + " reported warning(s)");
			messages.put("fr", errors + " erreur(s) reportée(s) et " + warnings + " avertissement(s) reporté(s).");

			for (String locale : messages.keySet()) {
				sched.error(rid, locale, messages.get(locale));
			}
		}

		if (warnings != 0) {
			Map<String, String> messages = new HashMap<>();
			messages.put("en", warnings + " reported warning(s)");
			messages.put("fr", warnings + " avertissement(s) reporté(s).");

			for (String locale : messages.keySet()) {
				sched.warn(rid, locale, messages.get(locale));
			}
		}
	}

	public JobExitStatus getJobStatus() {
		if (errors != 0) {
			return JobExitStatus.FAILURE;
		}

		if (warnings != 0) {
			return JobExitStatus.COMPLETED_WITH_WARNINGS;
		}

		return JobExitStatus.SUCCESS;
	}
}
