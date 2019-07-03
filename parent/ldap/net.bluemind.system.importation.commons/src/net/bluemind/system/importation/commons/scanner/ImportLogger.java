package net.bluemind.system.importation.commons.scanner;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.Optional;

import net.bluemind.scheduledjob.scheduler.IScheduledJobRunId;
import net.bluemind.scheduledjob.scheduler.IScheduler;

public class ImportLogger implements IImportLogger {
	private final Optional<IScheduler> sched;
	private final Optional<IScheduledJobRunId> rid;
	public final Optional<RepportStatus> repportStatus;

	public ImportLogger(Optional<IScheduler> sched, Optional<IScheduledJobRunId> rid,
			Optional<RepportStatus> repportStatus) {
		this.sched = sched;
		this.rid = rid;
		this.repportStatus = repportStatus;
	}

	public ImportLogger() {
		this.sched = Optional.empty();
		this.rid = Optional.empty();
		this.repportStatus = Optional.empty();
	}

	private boolean logEnabled() {
		if (!sched.isPresent() || !rid.isPresent()) {
			return false;
		}

		return true;
	}

	@Override
	public void info(Map<String, String> messages) {
		if (!logEnabled()) {
			return;
		}

		for (String locale : messages.keySet()) {
			sched.get().info(rid.get(), locale, messages.get(locale));
		}
	}

	@Override
	public void warning(Map<String, String> messages) {
		repportStatus.ifPresent(RepportStatus::warn);

		if (!logEnabled()) {
			return;
		}

		for (String locale : messages.keySet()) {
			sched.get().warn(rid.get(), locale, messages.get(locale));
		}
	}

	@Override
	public void error(Map<String, String> messages) {
		repportStatus.ifPresent(RepportStatus::error);

		if (!logEnabled()) {
			return;
		}

		for (String locale : messages.keySet()) {
			sched.get().error(rid.get(), locale, messages.get(locale));
		}
	}

	@Override
	public void reportException(Throwable t) {
		if (!logEnabled()) {
			return;
		}

		StringWriter sw = new StringWriter(1024);
		PrintWriter pw = new PrintWriter(sw, true);
		t.printStackTrace(pw);
		pw.close();
		String stack = sw.toString();

		sched.get().error(rid.get(), "en", "Exception occurred: " + t.getMessage() + "\n" + stack);
		sched.get().error(rid.get(), "fr", "Une exception est survenue: " + t.getMessage() + "\n" + stack);
	}

	@Override
	public ImportLogger withoutStatus() {
		return new ImportLogger(sched, rid, Optional.empty());
	}

	public void logStatus() {
		if (!logEnabled()) {
			return;
		}

		repportStatus.ifPresent(rs -> rs.logStatus(sched.get(), rid.get()));
	}
}
