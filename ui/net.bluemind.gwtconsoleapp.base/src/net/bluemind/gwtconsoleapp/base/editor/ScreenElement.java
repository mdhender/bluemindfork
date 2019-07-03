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
package net.bluemind.gwtconsoleapp.base.editor;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.core.client.JsonUtils;
import com.google.gwt.core.shared.GWT;

import net.bluemind.core.commons.gwt.JsMapStringString;

public class ScreenElement extends JavaScriptObject {

	protected ScreenElement() {
	}

	public final native String getId()
	/*-{
    return this["id"];
	}-*/;

	public final native String getType()
	/*-{
    return this["type"];
	}-*/;

	public final native String getRole()
	/*-{
    return this["role"];
	}-*/;

	public final native void setRole(String role)
	/*-{
    this["role"] = role;
	}-*/;

	public final native JsArrayString getRoles()
	/*-{
    return this["roles"];
	}-*/;

	public final native void setRoles(JsArrayString roles)
	/*-{
    this["roles"] = roles;
	}-*/;

	public final native JsMapStringString getAttributes()
	/*-{
    return this["attributes"];
	}-*/;

	public final native String getTitle()
	/*-{
    return this["title"];
	}-*/;

	public final native JsArrayString getActiveRoles()
	/*-{
    return this["activeRoles"] || [];
	}-*/;

	public final native void setTitle(String title)
	/*-{
    this["title"] = title;
	}-*/;

	public final native void setReadOnly(boolean b)
	/*-{
    this["readOnly"] = b;
	}-*/;

	public final native boolean isReadOnly()
	/*-{
    return this["readOnly"] == true;
	}-*/;

	public final static native void contribute(ScreenElement elt, String attribute, ScreenElement contribution)
	/*-{
    var curpart = $wnd;
    var parts = elt["type"].split('.');

    for (var i = 0; i < parts.length; i++) {
      var part = parts[i];
      if (!curpart[part]) {
        throw "class " + className + " doesnt exists";
      }
      curpart = curpart[part];
    }

    if (curpart["contribute"]) {
      curpart["contribute"](elt, attribute, contribution);
    } else {
      @com.google.gwt.core.client.GWT::log(Ljava/lang/String;)("contribute doesnt exists for "+elt["type"]);
    }
	}-*/;

	public final native void loadModel(JavaScriptObject model)
	/*-{
    this["loadModel"](model);
	}-*/;

	public final native void saveModel(JavaScriptObject model)
	/*-{
    this["saveModel"](model);
	}-*/;

	public final <T extends ScreenElement> T castElement(String type) {
		// if (ScreenElementTypes.instanceOf(getType(), type)) {
		return cast();
		// } else {
		// throw new ClassCastException("cannot cast " + getType() + " as " +
		// type);
		// }
	}

	private static native void rt()
	/*-{
    $wnd.bm.ScreenElement = function(model) {
      if (model) {
        this['id'] = model['id'];
        this['type'] = model['type'];
        this['attributes'] = model['attributes'];
        this['readOnly'] = model['readOnly'];
      }
    }

    $wnd.bm.ScreenElement.prototype.id = null;
    $wnd.bm.ScreenElement.prototype.type = null;

    $wnd.bm.ScreenElement.prototype.contribute = function() {
    }
    $wnd.bm.ScreenElement.prototype.saveModel = function() {
    }
    $wnd.bm.ScreenElement.prototype.loadModel = function() {
    }
	}-*/;

	public enum RoleStyle {
		empty, readOnly
	}

	public static RoleStyle style = RoleStyle.empty;

	public static ScreenElement build(ScreenElement model, EditorContext context) {
		JsArrayString activeRoles = JsArrayString.createArray().cast();
		boolean hasRole = true;
		if (model.getRole() != null && model.getRole().length() > 0) {
			hasRole = false;

			hasRole = contains(context.getRoles(), model.getRole());
			if (hasRole) {
				activeRoles.push(model.getRole());
			}
		}

		if (model.getRoles() != null && model.getRoles().length() > 0) {
			hasRole = false;
			for (int i = 0; i < model.getRoles().length(); i++) {
				String role = model.getRoles().get(i);
				boolean c = contains(context.getRoles(), role);
				hasRole = hasRole || c;
				if (c) {
					activeRoles.push(model.getRoles().get(i));
				}
			}
		}

		RoleStyle s = style;
		if (model.getAttributes() != null && model.getAttributes().get("security") != null) {
			s = RoleStyle.valueOf(model.getAttributes().get("security"));
		}
		if (!hasRole && s == RoleStyle.empty) {
			return null;
		} else if (!hasRole) {
			model.setReadOnly(true);
		} else {
			model.setActiveRoles(activeRoles);
		}

		GWT.log("built " + model.getType() + " readonly ? " + model.isReadOnly() + " json "
				+ JsonUtils.stringify(model));
		return JsHelper.construct(null, model.getType(), model, context);

	}

	private final native void setActiveRoles(JsArrayString activeRoles)
	/*-{
    this['activeRoles'] = activeRoles;
	}-*/;

	private static boolean contains(JsArrayString roles, String role) {
		for (int i = 0; i < roles.length(); i++) {
			if (roles.get(i).equals(role)) {
				return true;
			}
		}
		return false;
	}

	public static void registerType() {
		JsHelper.createPackage("bm");
		rt();
	}

	public final native ScreenElement withRole(String role)
	/*-{
    this['role'] = role;
    return this;
	}-*/;

	public final ScreenElement withRoles(String... roles) {
		JsArrayString r = JsArrayString.createArray().cast();
		for (String role : roles) {
			r.push(role);
		}

		return withRolesInternal(r);
	}

	private final native ScreenElement withRolesInternal(JsArrayString roles)
	/*-{
    this['roles'] = roles;
    return this;
	}-*/;

	public final native ScreenElement witTitle(String title)
	/*-{
    this['title'] = title;
    return this;
	}-*/;

	public final native ScreenElement readOnly()
	/*-{
    this['attributes'] = this['attributes'] || [];
    this['attributes']['security'] = 'readOnly';
    return this;
	}-*/;

	public static native ScreenElement create(String id, String type)
	/*-{
    return {
      'id' : id,
      'type' : type,
      'modelHandlers' : []
    };
	}-*/;

}
