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
package net.bluemind.core.auditlog.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.auditlog.AuditEvent;
import net.bluemind.core.auditlog.IAuditManager;
import net.bluemind.core.auditlog.appender.IAuditEventAppender;
import net.bluemind.core.auditlog.appender.slf4j.Slf4jEventAppender;

public class DefaultAuditManager implements IAuditManager {

	private static final Logger logger = LoggerFactory.getLogger(DefaultAuditManager.class);

	private CompositeAuditEventAppender rootAppender = new CompositeAuditEventAppender();

	public DefaultAuditManager() {
		rootAppender.addAppender(new Slf4jEventAppender());
	}

	@Override
	public void audit(AuditEvent event) {
		rootAppender.write(event);
	}

	public void addAppender(IAuditEventAppender appender) {
		logger.info("adding appender {}", appender);
		this.rootAppender.addAppender(appender);
	}
}
