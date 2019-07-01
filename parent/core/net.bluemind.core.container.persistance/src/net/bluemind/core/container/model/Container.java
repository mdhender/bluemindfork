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
package net.bluemind.core.container.model;

import java.util.Date;

public class Container {

	public long id;
	public String uid;
	public String type;
	public String name;
	public String owner;
	public String createdBy;
	public String updatedBy;
	public Date created;
	public Date updated;
	public String domainUid;
	public boolean defaultContainer;
	public boolean readOnly;

	public static Container create(String uid, String type, String name, String owner) {
		return create(uid, type, name, owner, null, false);
	}

	public static Container create(String uid, String type, String name, String owner, boolean defaultContainer) {
		return create(uid, type, name, owner, null, defaultContainer);
	}

	public static Container create(String uid, String type, String name, String owner, String domainUid) {
		return create(uid, type, name, owner, domainUid, false);
	}

	public static Container create(String uid, String type, String name, String owner, String domainUid,
			boolean defautlContainer) {
		Container ret = new Container();
		ret.uid = uid;
		ret.type = type;
		ret.name = name;
		ret.owner = owner;
		ret.domainUid = domainUid;
		ret.defaultContainer = defautlContainer;
		return ret;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((uid == null) ? 0 : uid.hashCode());
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
		Container other = (Container) obj;
		if (uid == null) {
			if (other.uid != null)
				return false;
		} else if (!uid.equals(other.uid))
			return false;
		return true;
	}

	public Container copy() {
		Container ret = new Container();
		ret.id = id;
		ret.uid = uid;
		ret.type = type;
		ret.name = name;
		ret.owner = owner;
		ret.createdBy = createdBy;
		ret.updatedBy = updatedBy;
		ret.created = created;
		ret.updated = updated;
		ret.domainUid = domainUid;
		ret.defaultContainer = defaultContainer;
		ret.readOnly = readOnly;

		return ret;
	}

	@Override
	public String toString() {
		return "Container [id=" + id + ", uid=" + uid + ", type=" + type + ", name=" + name + ", owner=" + owner
				+ ", createdBy=" + createdBy + ", updatedBy=" + updatedBy + ", created=" + created + ", updated="
				+ updated + ", domainUid=" + domainUid + ", defaultContainer=" + defaultContainer + ", readOnly="
				+ readOnly + "]";
	}

}
