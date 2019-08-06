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
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;

/**
 * Declaration of a role.
 */
@BMApi(version = "3")
public class RoleDescriptor {

	/**
	 * Unique identifier.
	 */
	public String id;
	/**
	 * Parent role, this role inherits all permissions of the parent role.
	 */
	public String parentRoleId;
	/**
	 * Associated {@link RoleCategory}.
	 */
	public String categoryId;
	/**
	 * Label.
	 */
	public String label;
	/**
	 * Role description.
	 */
	public String description;
	/**
	 * Role applies to own roles only.
	 */
	public boolean selfPromote;
	/**
	 * Role applies to associated directory entry.
	 */
	public boolean dirEntryPromote;
	/**
	 * Role applies to specific directory entry kind.
	 */
	public DirEntry.Kind dirEntryKind;
	/**
	 * Sibling role //FIXME explain
	 */
	public String siblingRole;
	/**
	 * Child roles.
	 */
	public Set<String> childsRole = new HashSet<>();
	/**
	 * Visible in administration.
	 */
	public boolean visible = true;
	/**
	 * Role can be delegated.
	 */
	public boolean delegable = false;
	/**
	 * Role applies to containers.
	 */
	public Set<String> containerRoles = new HashSet<>();
	/**
	 * Role priority //FIXME unused
	 */
	public int priority;

	public static RoleDescriptor create(String id, String categoryId, String label, String description) {
		RoleDescriptor ret = new RoleDescriptor();

		ret.id = id;
		ret.categoryId = categoryId;
		ret.label = label;
		ret.description = description;
		return ret;
	}

	/**
	 * Assign child roles.
	 * 
	 * @param roles
	 *                  Array of roles.
	 * @return A reference to the current instance (this).
	 */
	public RoleDescriptor giveRoles(String... roles) {
		this.childsRole = new HashSet<>(Arrays.asList(roles));
		return this;
	}

	/**
	 * Assign a parent role.
	 * 
	 * @param parentRoleId
	 *                         Parent role id.
	 * @return A reference to the current instance (this).
	 */
	public RoleDescriptor withParent(String parentRoleId) {
		this.parentRoleId = parentRoleId;
		return this;
	}

	/**
	 * Assign permission to own a role in the context of the specific owner of this
	 * role. A user owning this role will hava permission "parentRoleId" on the
	 * entities he owns.
	 * 
	 * @param parentRoleId
	 *                         Role identifier.
	 * @return A reference to the current instance (this).
	 */
	public RoleDescriptor withSelfPromote(String parentRoleId) {
		this.parentRoleId = parentRoleId;
		this.dirEntryKind = DirEntry.Kind.USER;
		this.selfPromote = true;
		return this;
	}

	/**
	 * Assign container roles.
	 * 
	 * @param roles
	 *                  Array of roles.
	 * @return A reference to the current instance (this).
	 */
	public RoleDescriptor withContainerRoles(String... roles) {
		this.containerRoles = new HashSet<>(Arrays.asList(roles));
		return this;
	}

	/**
	 * Hide this role.
	 * 
	 * @return A reference to the current instance (this).
	 */
	public RoleDescriptor notVisible() {
		this.visible = false;
		return this;
	}

	/**
	 * Make this role delegable.
	 * 
	 * @return A reference to the current instance (this).
	 */
	public RoleDescriptor delegable() {
		this.delegable = true;
		return this;
	}

	/**
	 * Assign a priority.
	 * 
	 * @param priority
	 * @return A reference to the current instance (this).
	 */
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
