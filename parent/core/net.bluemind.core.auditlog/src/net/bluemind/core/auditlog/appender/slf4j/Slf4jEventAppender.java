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

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import io.vertx.core.json.JsonObject;
import net.bluemind.core.auditlog.AuditEvent;
import net.bluemind.core.auditlog.appender.IAuditEventAppender;
import net.bluemind.core.utils.JsonUtils;

public class Slf4jEventAppender implements IAuditEventAppender {

	private static Logger logger = LoggerFactory.getLogger(Slf4jAuditLog.class);

	@Override
	public void write(AuditEvent event) {
		MDC.clear();
		MDC.put("object", event.getObject());
		MDC.put("readOnly", Boolean.toString(event.isReadOnly()));

		if (event.isReadOnly()) {
			logger.debug(toLog(new LogEntry(event)));
		} else if (event.succeed()) {
			logger.info(toLog(new LogEntry(event)));
		} else {
			logger.warn(toLog(new LogEntry(event, event.getFailure())));
		}
		MDC.clear();
	}

	private String toLog(LogEntry logEntry) {
		String asString = JsonUtils.asString(logEntry);
		JsonObject asObject = new JsonObject(asString);

		asObject.put("actionMeta", mapToJson(logEntry.actionMeta));
		asObject.put("objectMeta", mapToJson(logEntry.objectMeta));

		return asObject.encode();
	}

	private JsonObject mapToJson(Map<String, JsonObject> map) {
		JsonObject asObject = new JsonObject();
		map.entrySet().forEach(entry -> asObject.put(entry.getKey(), entry.getValue()));
		return asObject;
	}

}
