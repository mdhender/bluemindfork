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
package net.bluemind.cti.service.internal;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.cti.api.Status;
import net.bluemind.cti.api.Status.Type;

public class CTIStatusManager {

	private final static Logger logger = LoggerFactory.getLogger(CTIStatusManager.class);

	public class State {
		public ComponentStatus[] status;
		public ComponentForward[] forwards;

		public State(ComponentStatus[] status, ComponentForward[] forwards) {
			super();
			this.status = status;
			this.forwards = forwards;
		}

	}

	private static class ComponentForward {
		public final String component;
		public final int priority;
		public final String forward;

		public ComponentForward(String component, int priority, String forward) {
			this.component = component;
			this.priority = priority;
			this.forward = forward;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((component == null) ? 0 : component.hashCode());
			result = prime * result + ((forward == null) ? 0 : forward.hashCode());
			result = prime * result + priority;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ComponentForward other = (ComponentForward) obj;
			if (component == null) {
				if (other.component != null)
					return false;
			} else if (!component.equals(other.component))
				return false;
			if (forward == null) {
				if (other.forward != null)
					return false;
			} else if (!forward.equals(other.forward))
				return false;
			if (priority != other.priority)
				return false;
			return true;
		}

	}

	private class ComponentStatus {
		public final String component;
		public final String message;
		public final Status.Type type;
		public final int priority;

		public ComponentStatus(String component, int priority, Type type, String message) {
			this.component = component;
			this.message = message;
			this.type = type;
			this.priority = priority;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((component == null) ? 0 : component.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ComponentStatus other = (ComponentStatus) obj;
			if (component == null) {
				if (other.component != null)
					return false;
			} else if (!component.equals(other.component))
				return false;
			return true;
		}
	}

	private ConcurrentHashMap<String, State> stateMap = new ConcurrentHashMap<>();

	public String getForward(String domainUid, String uid) {
		return asForward(stateMap.get(uid + "@" + domainUid));
	}

	public Status getStatus(String domainUid, String uid) {
		return asStatus(stateMap.get(uid + "@" + domainUid));
	}

	public Status updateStatus(String domainUid, String uid, String component, Status status) {

		ComponentStatus cs = componentStatus(component, status);

		State s = stateMap.compute(uid + "@" + domainUid, (key, current) -> {
			if (current == null) {
				current = new State(new ComponentStatus[0], new ComponentForward[0]);
			}

			Set<ComponentStatus> statues = new HashSet<>(Arrays.asList(current.status));

			statues.remove(cs);
			statues.add(cs);

			ComponentStatus[] ns = statues.stream().filter(is -> {
				return is.type != Status.Type.Available;
			}).sorted((l, r) -> {
				return l.priority - r.priority;
			}).toArray(v -> {
				return new ComponentStatus[v];
			});

			return new State(ns, current.forwards);
		});

		return asStatus(s);

	}

	public String updateForward(String domainUid, String uid, String component, String forward) {

		ComponentForward cf = componentForward(component, forward);

		State s = stateMap.compute(uid + "@" + domainUid, (key, current) -> {
			if (current == null) {
				current = new State(new ComponentStatus[0], new ComponentForward[0]);
			}

			Set<ComponentForward> statues = new HashSet<>(Arrays.asList(current.forwards));

			statues.add(cf);

			ComponentForward[] ns = statues.stream().filter(is -> {
				return is.forward != null;
			}).sorted((l, r) -> {
				return l.priority - r.priority;
			}).toArray(v -> {
				return new ComponentForward[v];
			});

			if (ns.length == 0 && current.forwards.length == 0) {
				return null;
			} else {
				return new State(current.status, ns);
			}
		});

		return asForward(s);

	}

	private ComponentStatus componentStatus(String component, Status status) {
		return new ComponentStatus(component, priority(component), status.type, status.message);
	}

	private ComponentForward componentForward(String component, String forward) {
		return new ComponentForward(component, priority(component), forward);
	}

	private int priority(String component) {
		if ("IM".equals(component)) {
			return 10;
		} else if ("Calendar".equals(component)) {
			return 5;
		} else {
			return 0;
		}
	}

	private Status asStatus(State state) {
		if (state == null) {
			return Status.create(Type.Available, null);
		} else {
			return asStatus(state.status);
		}
	}

	private Status asStatus(ComponentStatus[] statues) {
		Status ret = new Status();
		ret.type = Type.Available;
		if (statues != null && statues.length > 0) {
			ret.type = statues[0].type;
			ret.message = statues[0].message;
		}
		return ret;
	}

	private String asForward(State state) {
		if (state == null) {
			return null;
		} else {
			return asForward(state.forwards);
		}
	}

	private String asForward(ComponentForward[] forwards) {
		String ret = null;
		if (forwards != null && forwards.length > 0) {
			ret = forwards[0].forward;
		}
		return ret;
	}

}
