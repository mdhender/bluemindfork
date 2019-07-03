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

import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;

public class DirEntryPermission implements Permission {

	public static Permission create(Kind kind, String verb) {
		return new DirEntryPermission(kind, verb);
	}

	private final DirEntry.Kind kind;

	private final String verb;

	public DirEntryPermission(DirEntry.Kind kind, String permission) {
		this.kind = kind;
		this.verb = permission;
	}

	@Override
	public boolean implies(Permission perm) {
		if (!(perm instanceof DirEntryPermission)) {
			return false;
		}

		DirEntryPermission tperm = (DirEntryPermission) perm;

		return verb.equals(tperm.verb);
	}

	public Kind getKind() {
		return kind;
	}

	@Override
	public String toString() {
		return "DirEntryPermission [kind=" + kind + ", verb=" + verb + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((kind == null) ? 0 : kind.hashCode());
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
		DirEntryPermission other = (DirEntryPermission) obj;
		if (kind != other.kind)
			return false;
		if (verb == null) {
			if (other.verb != null)
				return false;
		} else if (!verb.equals(other.verb))
			return false;
		return true;
	}

	@Override
	public String asRole() {
		return verb;
	}

}
