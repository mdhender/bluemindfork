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
package net.bluemind.core.auditlog;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import io.vertx.core.json.JsonObject;

public class AuditEvent {

	private String actor;
	private Map<String, String> actorMetadatas = new HashMap<>();
	private String action;
	private Map<String, JsonObject> actionMetadatas = new HashMap<>();
	private String object;
	private Map<String, JsonObject> objectMetadatas = new HashMap<>();
	private AuditEvent parent;
	private Throwable failure;
	private final String eventId;
	private String parentEventId;
	private boolean readOnly = false;
	private long timestamp;

	public AuditEvent() {
		this(null);
	}

	public AuditEvent(AuditEvent parent) {
		timestamp = System.currentTimeMillis();
		eventId = UUID.randomUUID().toString();
		this.parent = parent;
		if (parent != null) {
			this.actor = parent.actor;
			this.actorMetadatas = new HashMap<>(parent.actorMetadatas);
			this.object = parent.object;
			this.objectMetadatas = new HashMap<>(parent.objectMetadatas);
		}
	}

	public long getTimestamp() {
		return timestamp;
	}

	public String getId() {
		return eventId;
	}

	public void setActor(String actor) {
		this.actor = actor;
	}

	public String getActor() {
		return actor;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getAction() {
		return action;
	}

	public void setObject(String object) {
		this.object = object;
	}

	public String getObject() {
		return object;
	}

	public void readOnly() {
		this.readOnly = true;
	}

	public void readOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	public void addActorMetadata(String key, String value) {
		actorMetadatas.put(key, value);
	}

	public void addObjectMetadata(String key, JsonObject value) {
		objectMetadatas.put(key, value);

	}

	public void addActionMetadata(String key, JsonObject value) {
		actionMetadatas.put(key, value);
	}

	public AuditEvent createChildEvent() {
		AuditEvent ret = new AuditEvent(this);
		return ret;
	}

	public AuditEvent getParent() {
		return parent;
	}

	public void setParentEventId(String parentEventId) {
		this.parentEventId = parentEventId;
		this.parent = null;
	}

	public String getParentEventId() {
		if (parent == null) {
			return parentEventId;
		} else {
			return parent.getId();
		}
	}

	public void actionSucceed() {
	}

	public void actionFailed(Throwable cause) {
		this.failure = cause;
	}

	public Throwable getFailure() {
		return failure;
	}

	public boolean succeed() {
		return failure == null;
	}

	public Map<String, String> getActorMeta() {
		return actorMetadatas;
	}

	public Map<String, JsonObject> getActionMeta() {
		return actionMetadatas;
	}

	public Map<String, JsonObject> getObjectMeta() {
		return objectMetadatas;
	}

}
