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
package net.bluemind.group.api;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class Member {
	public Type type;
	public String uid;

	@BMApi(version = "3")
	public static enum Type {
		user, group, external_user;
	}

	public static Member user(String uid) {
		return create(uid, Type.user);
	}

	public static Member group(String uid) {
		return create(uid, Type.group);
	}
	
	public static Member externalUser(String uid) {
		return create(uid, Type.external_user);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((type == null) ? 0 : type.hashCode());
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
		Member other = (Member) obj;
		if (type != other.type)
			return false;
		if (uid == null) {
			if (other.uid != null)
				return false;
		} else if (!uid.equals(other.uid))
			return false;
		return true;
	}

	private static Member create(String uid, Type type) {
		Member ret = new Member();
		ret.uid = uid;
		ret.type = type;
		return ret;
	}

	@Override
	public String toString() {
		return "Member [type=" + type + ", uid=" + uid + "]";
	}
}
