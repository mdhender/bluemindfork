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
package net.bluemind.ui.gwtrole.client;

import java.util.Set;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;

import net.bluemind.core.commons.gwt.GwtSerDerUtils;
import net.bluemind.role.api.RoleDescriptor;
import net.bluemind.role.api.RolesCategory;
import net.bluemind.role.api.gwt.js.JsRoleDescriptor;
import net.bluemind.role.api.gwt.js.JsRolesCategory;
import net.bluemind.role.api.gwt.serder.RoleDescriptorGwtSerDer;
import net.bluemind.role.api.gwt.serder.RolesCategoryGwtSerDer;

public class RolesModel extends JavaScriptObject {

	protected RolesModel() {
	}

	public final native JsArrayString getInheritedRoles()
	/*-{
	return this['inherited-roles'];
	}-*/;

	public final native void setInheritedRoles(JsArrayString roles)
	/*-{
	this['inherited-roles'] = roles;
	}-*/;

	public final native JsArrayString getRoles()
	/*-{
	return this['entity-roles'];
	}-*/;

	public final native void setRoles(JsArrayString roles)
	/*-{
	this['entity-roles'] = roles;
	}-*/;

	public final native JsArray<JsRoleDescriptor> getDescciptors()
	/*-{
	return this['roles'];
	}-*/;

	public final native JsArray<JsRolesCategory> getCategories()
	/*-{
	return this['rolesCategories'];
	}-*/;

	public final native JsArrayString getReadOnlyRoles()
	/*-{
	return this['readonly-roles'];
	}-*/;

	public final native void setReadOnlyRoles(JsArrayString roles)
	/*-{
	this['readonly-roles'] = roles;
	}-*/;

	public final native void setReadOnly(boolean readOnly)
	/*-{
	this['readOnly'] = readOnly;
	}-*/;

	public final native boolean isReadOnly()
	/*-{
	return this['readOnly'];
	}-*/;

	public final void setNativeRoles(Set<RoleDescriptor> value) {
		JsArray<JsRoleDescriptor> roles = new GwtSerDerUtils.CollectionSerDer<RoleDescriptor>(
				new RoleDescriptorGwtSerDer()).serialize(value).isArray().getJavaScriptObject().cast();
		setDescriptors(roles);
	}

	public final void setNativeCategories(Set<RolesCategory> value) {
		JsArray<JsRolesCategory> roles = new GwtSerDerUtils.CollectionSerDer<RolesCategory>(
				new RolesCategoryGwtSerDer()).serialize(value).isArray().getJavaScriptObject().cast();
		setCategories(roles);
	}

	public final native void setDescriptors(JsArray<JsRoleDescriptor> roles)
	/*-{
	this['roles'] = roles;
	}-*/;

	public final native void setCategories(JsArray<JsRolesCategory> categories)
	/*-{
	this['rolesCategories'] = categories;
	}-*/;
}
