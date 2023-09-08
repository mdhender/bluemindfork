package net.bluemind.osgi.log;

import org.eclipse.equinox.log.ExtendedLogEntry;
import org.eclipse.equinox.log.ExtendedLogReaderService;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SLF4JLogAdapter {

	private Logger logger = LoggerFactory.getLogger("OSGI");

	public void activate() {
		logger.info("SLF4J is activated !");
	}

	public void deactivate() {
		logger.debug("deactivation");
	}

	public void addExtendedLogReaderService(ExtendedLogReaderService elrs) {
		elrs.addLogListener(new LogListener() {

			@Override
			public void logged(LogEntry entry) {
				if (!(entry instanceof ExtendedLogEntry))
					return;
				ExtendedLogEntry extended = (ExtendedLogEntry) entry;
				Object context = extended.getContext();
				if (context instanceof FrameworkLogEntry contextLogEntry) {
					log(0, contextLogEntry);
					return;

				}
				String fmt = "bundle {} : {}";
				switch (entry.getLogLevel()) {
				case ERROR:
					logger.error(fmt, entry.getBundle(), entry.getMessage(), entry.getException());
					break;
				case INFO:
					logger.debug(fmt, entry.getBundle(), entry.getMessage());
					break;
				case DEBUG:
					logger.debug(fmt, entry.getBundle(), entry.getMessage());
					break;
				case WARN:
					logger.warn(fmt, entry.getBundle(), entry.getMessage());
					break;
				default:
					break;
				}
			}

			private void log(int depth, FrameworkLogEntry entry) {

				switch (entry.getSeverity()) {
				case FrameworkLogEntry.ERROR:
					logger.error(entry.getMessage());
					break;
				case FrameworkLogEntry.WARNING:
					logger.warn(entry.getMessage());
					break;
				default:
					logger.info(entry.getMessage());
					break;
				}
				FrameworkLogEntry[] children = entry.getChildren();
				if (children != null) {
					for (int i = 0; i < children.length; i++) {
						log(depth + 1, children[i]);
					}
				}

			}

		});
	}

	public void addLogReaderService(LogReaderService logReaderService) {
		logReaderService.addLogListener(new LogListener() {

			@Override
			public void logged(LogEntry entry) {
				switch (entry.getLogLevel()) {
				case ERROR:
					logger.error("bundle {} : {}", entry.getBundle(), entry.getMessage());
					break;
				case INFO:
					logger.info("bundle {} : {}", entry.getBundle(), entry.getMessage());
					break;
				case DEBUG:
					logger.debug("bundle {} : {}", entry.getBundle(), entry.getMessage());
					break;
				case WARN:
					logger.warn("bundle {} : {}", entry.getBundle(), entry.getMessage());
					break;
				default:
					break;
				}
			}
		});
	}
}
