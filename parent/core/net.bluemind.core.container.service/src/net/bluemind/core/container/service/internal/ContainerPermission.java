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
package net.bluemind.core.container.service.internal;

import net.bluemind.core.container.model.acl.Verb;

public class ContainerPermission implements Permission {

	public static final ContainerPermission INVIVATION = new ContainerPermission(Verb.Invitation);
	public static final ContainerPermission FREEBUSY = new ContainerPermission(Verb.Freebusy);
	public static final ContainerPermission SEND_ON_BEHALF = new ContainerPermission(Verb.SendOnBehalf);
	public static final ContainerPermission READ = new ContainerPermission(Verb.Read);
	public static final ContainerPermission WRITE = new ContainerPermission(Verb.Write);
	public static final ContainerPermission MANAGE = new ContainerPermission(Verb.Manage);
	public static final ContainerPermission ALL = new ContainerPermission(Verb.All);

	private final Verb verb;

	public ContainerPermission(Verb verb) {
		this.verb = verb;
	}

	@Override
	public boolean implies(Permission perm) {
		if (!(perm instanceof ContainerPermission)) {
			return false;
		}

		ContainerPermission cp = (ContainerPermission) perm;

		return verb.can(cp.verb);
	}

	@Override
	public String toString() {
		return "ContainerPermission [verb=" + verb + "]";
	}

	public static Permission asPerm(String v) {
		return asPerm(Verb.valueOf(v));
	}

	public static Permission asPerm(Verb v) {
		switch (v) {
		case Invitation:
			return INVIVATION;
		case Freebusy:
			return FREEBUSY;
		case SendOnBehalf:
			return SEND_ON_BEHALF;
		case Read:
			return READ;
		case Write:
			return WRITE;
		case Manage:
			return MANAGE;
		case All:
			return ALL;
		default:
			return null;
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((verb == null) ? 0 : verb.hashCode());
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
		ContainerPermission other = (ContainerPermission) obj;
		if (verb != other.verb)
			return false;
		return true;
	}

	@Override
	public String asRole() {
		return verb.name();
	}

}
