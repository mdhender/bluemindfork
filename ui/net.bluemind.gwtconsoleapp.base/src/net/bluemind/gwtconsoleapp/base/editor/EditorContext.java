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

import java.util.Collection;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;

import net.bluemind.core.commons.gwt.JsMapStringJsObject;

public class EditorContext extends JavaScriptObject {

	protected EditorContext() {

	}

	public final native JsArrayString getRoles()
	/*-{
	return this.roles;
	}-*/;

	public static EditorContext create(Collection<String> roles) {
		JsArrayString rr = JavaScriptObject.createArray().cast();
		for (String r : roles) {
			rr.push(r);
		}

		JsMapStringJsObject ret = JavaScriptObject.createObject().cast();
		ret.put("roles", rr);
		return ret.cast();
	}
}
