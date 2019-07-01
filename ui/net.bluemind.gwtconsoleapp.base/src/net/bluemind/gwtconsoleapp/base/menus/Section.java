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
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;

public class Section extends JavaScriptObject {

	protected Section() {

	}

	public final native String getId()
	/*-{
	return this.id;
	}-*/;

	public final native String getName()
	/*-{
	return this.name;
	}-*/;

	public final native int getPriority()
	/*-{
	if (this.priority) {
	  return this.priority;
	} else {
	  return 0;
	}
	}-*/;

	public final native String getIconStyle()
	/*-{
	return this.iconStyle;
	}-*/;

	public final native JsArray<Screen> getScreens()
	/*-{
	return this.screens;
	}-*/;

	public final native JsArray<Section> getSections()
	/*-{
	return this.sections;
	}-*/;

	public final native JsArrayString getRoles()
	/*-{
	return this.roles;
	}-*/;

	public final native void setRoles(JsArrayString roles)
	/*-{
	this.roles = roles;
	}-*/;

	public static final native Section create(String id, String name, int priority, String iconStyle,
			JsArray<Screen> screens, JsArray<Section> subSections)
			/*-{
			return {
			'id' : id,
			'name' : name,
			'priority' : priority,
			'iconStyle' : iconStyle,
			'screens' : screens,
			'sections' : subSections,
			'roles' : []
			
			};
			}-*/;

	public static final native Section createSimple(String id, String name, String iconStyle, JsArray<Screen> screens)
	/*-{
	return {
	  'id' : id,
	  'name' : name,
	  'iconStyle' : iconStyle,
	  'screens' : screens,
	  'sections' : [],
	  'roles' : []
	
	};
	}-*/;

	public static final native Section createVerySimple(String id, String name, String iconStyle)
	/*-{
	return {
	  'id' : id,
	  'name' : name,
	  'iconStyle' : iconStyle,
	  'screens' : [],
	  'sections' : [],
	  'roles' : []
	
	};
	}-*/;

	public static final native Section createWithPriority(String id, String name, int priority)
	/*-{
	return {
	  'id' : id,
	  'name' : name,
	  'priority' : priority,
	  'iconStyle' : null,
	  'screens' : [],
	  'sections' : [],
	  'roles' : []
	
	};
	}-*/;

}
