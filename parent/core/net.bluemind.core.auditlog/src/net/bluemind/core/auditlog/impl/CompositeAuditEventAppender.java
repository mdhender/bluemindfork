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

import java.util.List;

import com.google.common.collect.ImmutableList;

import net.bluemind.core.auditlog.AuditEvent;
import net.bluemind.core.auditlog.appender.IAuditEventAppender;

public class CompositeAuditEventAppender implements IAuditEventAppender {

	private List<IAuditEventAppender> appenders = ImmutableList.of();

	public void addAppender(IAuditEventAppender appender) {
		this.appenders = ImmutableList.<IAuditEventAppender>builder().addAll(appenders).add(appender).build();
	}

	@Override
	public void write(AuditEvent event) {
		for (IAuditEventAppender appender : appenders) {
			appender.write(event);
		}
	}
}
