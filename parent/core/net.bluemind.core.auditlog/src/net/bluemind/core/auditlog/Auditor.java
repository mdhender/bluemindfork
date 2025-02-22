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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Throwables;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.utils.JsonUtils;

public class Auditor<T extends Auditor<T>> {

	private AuditEvent event;
	private IAuditManager manager;

	public Auditor(IAuditManager manager) {
		this.event = new AuditEvent();
		this.manager = manager;
	}

	public T readOnly() {
		event.readOnly();
		return getThis();
	}

	public T readOnly(boolean readOnly) {
		event.readOnly(readOnly);
		return getThis();
	}

	public T parentEventId(String auditEventId) {
		event.setParentEventId(auditEventId);
		return getThis();
	}

	public String eventId() {
		return event.getId();
	}

	public void auditSuccess() {
		event.actionSucceed();
		manager.audit(event);
		finishCurrentEvent();
	}

	public void auditFailure(Throwable cause) {
		event.actionFailed(cause);
		manager.audit(event);
		finishCurrentEvent();
	}

	private void finishCurrentEvent() {
		final AuditEvent currentEvent = event;
		event = currentEvent.getParent();
		if (event == null) {
			// create an new AuditEvent but keep the event object
			event = new AuditEvent(currentEvent);
		}
	}

	public T actor(String actor) {
		event.setActor(actor);
		return getThis();
	}

	public T action(String action) {
		event.setAction(action);
		return getThis();
	}

	public T object(String object) {
		event.setObject(object);
		return getThis();
	}

	public T forContext(BmContext context) {
		return forSecurityContext(context.getSecurityContext());
	}

	public T forSecurityContext(SecurityContext context) {
		actor(context.getSubject() + "@" + context.getContainerUid());
		event.addActorMetadata("remote", String.join(",", context.getRemoteAddresses()));
		event.addActorMetadata("session", context.getSessionId());
		event.addActorMetadata("origin", context.getOrigin());
		return getThis();
	}

	public T addObjectMetadata(String key, Object value) {
		if (value != null) {
			event.addObjectMetadata(key, valueAsObject(value));
		} else {
			event.addObjectMetadata(key, null);
		}

		return getThis();
	}

	public T addActionMetadata(String key, Object value) {
		if (value != null) {
			event.addActionMetadata(key, valueAsObject(value));
		} else {
			event.addActionMetadata(key, null);
		}
		return getThis();
	}

	private JsonObject valueAsObject(Object value) {
		if (isPrimitive(value)) {
			return primitiveValueToJson(value);
		} else if (value instanceof List) {
			JsonObject val = new JsonObject();
			val.put("values", new JsonArray((List<?>) value));
			return val;
		} else {
			try {
				return JsonObject.mapFrom(value);
			} catch (Exception e) {
				return primitiveValueToJson("Undecodable value : " + e.getMessage());
			}
		}
	}

	private JsonObject primitiveValueToJson(Object value) {
		Map<String, Object> params = new HashMap<>();
		params.put("value", value);
		String payload = JsonUtils.asString(params);
		return new JsonObject(payload);
	}

	@FunctionalInterface
	public interface AuditedFunc<R> {

		public R apply() throws ServerFault;
	}

	@FunctionalInterface
	public interface AuditedProc {
		public void apply() throws ServerFault;
	}

	public <Res> Res audit(AuditedFunc<Res> func) {
		try {
			Res ret = func.apply();
			auditSuccess();
			return ret;
		} catch (ServerFault e) {
			auditFailure(e);
			throw e;
		} catch (Exception e) {
			auditFailure(e);
			throw Throwables.propagate(e);
		}
	}

	public void audit(AuditedProc func) {
		try {
			func.apply();
			auditSuccess();
		} catch (ServerFault e) {
			auditFailure(e);
			throw e;
		} catch (Exception e) {
			auditFailure(e);
			throw Throwables.propagate(e);
		}
	}

	@SuppressWarnings("unchecked")
	protected final T getThis() {
		return (T) this;
	}

	public T subAction() {
		event = event.createChildEvent();
		return getThis();
	}

	private static final Set<Class<?>> wrapperTypes = getWrapperTypes();

	private static boolean isPrimitive(Object obj) {
		return wrapperTypes.contains(obj.getClass());
	}

	private static Set<Class<?>> getWrapperTypes() {
		Set<Class<?>> ret = new HashSet<>();
		ret.add(String.class);
		ret.add(Boolean.class);
		ret.add(Character.class);
		ret.add(Byte.class);
		ret.add(Short.class);
		ret.add(Integer.class);
		ret.add(Long.class);
		ret.add(Float.class);
		ret.add(Double.class);
		return ret;
	}

}
