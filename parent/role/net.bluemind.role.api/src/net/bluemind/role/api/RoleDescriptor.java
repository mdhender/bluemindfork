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
package net.bluemind.role.api;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import net.bluemind.core.api.BMApi;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.BaseDirEntry.Kind;

@BMApi(version = "3")
public class RoleDescriptor {

	public String id;

	public String parentRoleId;

	public String categoryId;

	public String label;

	public String description;

	public boolean selfPromote;

	public boolean dirEntryPromote;

	public DirEntry.Kind dirEntryKind;

	public String siblingRole;

	public Set<String> childsRole = new HashSet<>();

	public boolean visible = true;

	public boolean delegable = false;

	public Set<String> containerRoles = new HashSet<>();

	public int priority;

	public static RoleDescriptor create(String id, String categoryId, String label, String description) {
		RoleDescriptor ret = new RoleDescriptor();

		ret.id = id;
		ret.categoryId = categoryId;
		ret.label = label;
		ret.description = description;
		return ret;
	}

	public RoleDescriptor giveRoles(String... roles) {
		this.childsRole = new HashSet<>(Arrays.asList(roles));
		return this;
	}

	public RoleDescriptor withParent(String parentRoleId) {
		this.parentRoleId = parentRoleId;
		return this;
	}

	public RoleDescriptor withSelfPromote(String parentRoleId) {
		this.parentRoleId = parentRoleId;
		this.dirEntryKind = DirEntry.Kind.USER;
		this.selfPromote = true;
		return this;
	}

	public RoleDescriptor withContainerRoles(String... roles) {
		this.containerRoles = new HashSet<>(Arrays.asList(roles));
		return this;
	}

	public RoleDescriptor notVisible() {
		this.visible = false;
		return this;
	}

	public RoleDescriptor delegable() {
		this.delegable = true;
		return this;
	}

	public RoleDescriptor priotity(int priority) {
		this.priority = priority;
		return this;
	}

	public RoleDescriptor forDirEntry(DirEntry.Kind kind, String siblingRole, String parentId) {
		this.parentRoleId = parentId;
		this.dirEntryKind = kind;
		this.siblingRole = siblingRole;
		this.dirEntryPromote = true;
		return this;
	}

	public RoleDescriptor forDirEntry(Kind kind) {
		this.dirEntryKind = kind;
		this.dirEntryPromote = true;
		return this;
	}

}
