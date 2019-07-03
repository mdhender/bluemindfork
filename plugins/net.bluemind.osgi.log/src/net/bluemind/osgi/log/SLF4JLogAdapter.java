package net.bluemind.osgi.log;

import org.eclipse.equinox.log.ExtendedLogEntry;
import org.eclipse.equinox.log.ExtendedLogReaderService;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.log.LogService;
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
				if (context instanceof FrameworkLogEntry) {
					log(0, (FrameworkLogEntry) context);
					return;

				}
				switch (entry.getLevel()) {
				case LogService.LOG_ERROR:
					logger.error("bundle {} : {}", entry.getBundle(), entry.getMessage(), entry.getException());
					break;
				case LogService.LOG_INFO:
					logger.debug("bundle {} : {}", entry.getBundle(), entry.getMessage());
					break;
				case LogService.LOG_DEBUG:
					logger.debug("bundle {} : {}", entry.getBundle(), entry.getMessage());
					break;
				case LogService.LOG_WARNING:
					logger.warn("bundle {} : {}", entry.getBundle(), entry.getMessage());
					break;
				default:
					break;
				}
			}

			private void log(int depth, FrameworkLogEntry entry) {

				switch (entry.getSeverity()) {
				case FrameworkLogEntry.INFO:
					logger.info(entry.getMessage());
					break;
				case FrameworkLogEntry.ERROR:
					logger.error(entry.getMessage());
					break;
				case FrameworkLogEntry.WARNING:
					logger.warn(entry.getMessage());

					break;
				case FrameworkLogEntry.CANCEL:
					logger.info(entry.getMessage());

					break;

				case FrameworkLogEntry.OK:
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
				switch (entry.getLevel()) {
				case LogService.LOG_ERROR:
					logger.error("bundle {} : {}", entry.getBundle(), entry.getMessage());
					break;
				case LogService.LOG_INFO:
					logger.info("bundle {} : {}", entry.getBundle(), entry.getMessage());
					break;
				case LogService.LOG_DEBUG:
					logger.debug("bundle {} : {}", entry.getBundle(), entry.getMessage());
					break;
				case LogService.LOG_WARNING:
					logger.warn("bundle {} : {}", entry.getBundle(), entry.getMessage());
					break;
				default:
					break;
				}
			}
		});
	}
}
