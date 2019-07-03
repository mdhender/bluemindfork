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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import net.bluemind.core.auditlog.AuditEvent;
import net.bluemind.core.auditlog.appender.IAuditEventAppender;

public class Slf4jEventAppender implements IAuditEventAppender {

	private static Logger logger = LoggerFactory.getLogger(Slf4jAuditLog.class);

	@Override
	public void write(AuditEvent event) {
		MDC.clear();
		MDC.put("object", event.getObject());
		MDC.put("readOnly", Boolean.toString(event.isReadOnly()));

		if (event.isReadOnly()) {
			logger.debug("{} {} : (actor:{} meta: {}) -> (action:{}, ro:{}, meta: {}) on (object:{} meta: {}) succeed",
					event.getTimestamp(), event.getId(), filter(event.getActor()), filter(event.getActorMeta()), //
					filter(event.getAction()), event.isReadOnly(), filter(event.getActionMeta()),
					filter(event.getObject()), filter(event.getObjectMeta()));
		} else if (event.succeed()) {
			logger.info("{} {} : (actor:{} meta: {}) -> (action:{}, ro:{}, meta: {}) on (object:{} meta: {}) succeed",
					event.getTimestamp(), event.getId(), filter(event.getActor()), filter(event.getActorMeta()), //
					filter(event.getAction()), event.isReadOnly(), filter(event.getActionMeta()),
					filter(event.getObject()), filter(event.getObjectMeta()));
		} else {
			logger.warn("{} {} : (actor:{} meta: {}) -> (action:{}, ro:{}, meta: {}) on (object:{} meta: {}) failed",
					event.getTimestamp(), event.getId(), filter(event.getActor()), filter(event.getActorMeta()), //
					filter(event.getAction()), event.isReadOnly(), filter(event.getActionMeta()),
					filter(event.getObject()), filter(event.getObjectMeta()), event.getFailure());
		}
		MDC.clear();
	}

	private Map<String, String> filter(Map<String, String> data) {
		Map<String, String> filtered = new HashMap<>();
		for (Entry<String, String> entry : data.entrySet()) {
			String key = filter(entry.getKey());
			String value = filter(entry.getValue());
			filtered.put(key, value);
		}
		return filtered;
	}

	private String filter(String data) {
		if (data == null) {
			return null;
		}
		data = data.replace("(", "[");
		data = data.replace(")", "]");
		return data;
	}

}
