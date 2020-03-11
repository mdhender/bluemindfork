/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import io.vertx.core.json.JsonObject;
import net.bluemind.core.auditlog.AuditEvent;

public class LogEntry {

	private final DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
	public final String timestamp;
	public final String eventId;
	public final String actor;
	public final Map<String, String> actorMeta;
	public final String action;
	public final Map<String, JsonObject> actionMeta;
	public final String object;
	public final Map<String, JsonObject> objectMeta;
	public final boolean readOnly;
	public final String error;

	LogEntry(AuditEvent event) {
		this(event, null);
	}

	LogEntry(AuditEvent event, Throwable error) {
		Instant instant = Instant.ofEpochMilli(event.getTimestamp());
		LocalDateTime date = instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
		this.timestamp = formatter.format(date);
		this.eventId = event.getId();
		this.actor = event.getActor();
		this.actorMeta = event.getActorMeta();
		this.action = event.getAction();
		this.actionMeta = event.getActionMeta();
		this.object = event.getObject();
		this.objectMeta = event.getObjectMeta();
		this.readOnly = event.isReadOnly();
		this.error = toStack(error);
	}

	private String toStack(Throwable error) {
		if (error == null) {
			return null;
		}
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		error.printStackTrace(pw);
		return sw.toString();
	}

	public String getTimestamp() {
		return timestamp;
	}

	public String getEventId() {
		return eventId;
	}

	public String getActor() {
		return actor;
	}

	public String getAction() {
		return action;
	}

	public String getObject() {
		return object;
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	public String getError() {
		return error;
	}

	public Map<String, String> getActorMeta() {
		return actorMeta;
	}

}
