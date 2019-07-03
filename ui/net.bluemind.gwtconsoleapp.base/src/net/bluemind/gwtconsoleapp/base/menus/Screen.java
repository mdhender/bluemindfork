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
package net.bluemind.gwtconsoleapp.base.menus;

import com.google.gwt.core.client.JavaScriptObject;

public final class Screen extends JavaScriptObject {

	protected Screen() {

	}

	public final native String getId()
	/*-{
    return this.id;
	}-*/;

	public final native String getName()
	/*-{
    return this.name;
	}-*/;

	public final native String[] getRoles()
	/*-{
    return this.roles;
	}-*/;

	public final native String[] getOURoles()
	/*-{
    return this.ouRoles;
	}-*/;

	public final native boolean isTopLevel()
	/*-{
    return this.topLevel;
	}-*/;

	public final native boolean isDirEntryEditor()
	/*-{
    return this.dirEntryEditor;
	}-*/;

	public final native Screen withRoles(String... roles)
	/*-{
    this.roles = roles;
    return this;
	}-*/;

	public final native Screen withOURoles(String... roles)
	/*-{
    this.ouRoles = roles;
    return this;
	}-*/;

	public static final native Screen create(String id, String name, boolean topLevel)
	/*-{
    return {
      'id' : id,
      'name' : name,
      'roles' : null,
      'topLevel' : topLevel,
      'dirEntryEditor' : false
    };
	}-*/;

	public static final native Screen create(String id, String name, String role, boolean topLevel)
	/*-{
    return {
      'id' : id,
      'name' : name,
      'roles' : (role ? [ role ] : null),
      'topLevel' : topLevel,
      'dirEntryEditor' : false
    };
	}-*/;

	public static final native Screen createDirEditor(String id, String name, String role, boolean topLevel)
	/*-{
    return {
      'id' : id,
      'name' : name,
      'roles' : (role ? [ role ] : null),
      'topLevel' : topLevel,
      'dirEntryEditor' : true
    };
	}-*/;
}
