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
package net.bluemind.core.auditlog.appender.slf4j;

import java.io.File;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;

public interface Slf4jAuditLog {

	static void init() {
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		File confFile = null;
		if (new File("/etc/bm/local/bm-core-audit.log.xml").exists()) {
			confFile = new File("/etc/bm/local/bm-core-audit.log.xml");
		} else {
			confFile = new File("/usr/share/bm-conf/logs/bm-core-audit.log.xml");
		}
		try {
			JoranConfigurator configurator = new JoranConfigurator();
			configurator.setContext(context);
			configurator.doConfigure(confFile.getAbsolutePath());
		} catch (JoranException je) {
			LoggerFactory.getLogger(Slf4jAuditLog.class).error("error audit log init: {}", je.getMessage());
		}
	}

}
