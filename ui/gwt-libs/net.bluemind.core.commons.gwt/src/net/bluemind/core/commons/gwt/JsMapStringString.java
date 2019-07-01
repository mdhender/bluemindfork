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
package net.bluemind.core.commons.gwt;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;

public class JsMapStringString extends JavaScriptObject {
	protected JsMapStringString() {
	}

	public final native String get(String key)
	/*-{
		return this[key];
	}-*/;

	public final native void put(String key, String value)
	/*-{
		this[key] = value;
	}-*/;

	public final native void remove(String key)
	/*-{
		delete this[key];
	}-*/;

	public final Map<String, String> asMap() {
		HashMap<String, String> ret = new HashMap<>();
		JsArrayString keys = keys();
		for (int i = 0; i < keys.length(); i++) {
			ret.put(keys.get(i), get(keys.get(i)));
		}
		return ret;
	}

	public static JsMapStringString create(Map<String, String> map) {
		JsMapStringString ret = JavaScriptObject.createObject().cast();
		for (Map.Entry<String, String> entry : map.entrySet()) {
			ret.put(entry.getKey(), entry.getValue());
		}
		return ret;
	}

	public final native JsArrayString keys()
	/*-{
		var ret = [];
		for ( var key in this) {
			if (this.hasOwnProperty(key)) {

				ret.push(key);
			}
		}
		return ret;
	}-*/;
}
