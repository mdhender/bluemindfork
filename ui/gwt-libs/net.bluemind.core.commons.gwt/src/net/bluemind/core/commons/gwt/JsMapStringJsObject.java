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

import java.util.Map;

import com.google.gwt.core.client.JavaScriptObject;

public class JsMapStringJsObject extends JavaScriptObject {
	protected JsMapStringJsObject() {
	}

	public final native JavaScriptObject get(String key)
	/*-{
    return this[key];
	}-*/;

	public final native <T> T getObject(String key)
	/*-{
    return this[key];
	}-*/;

	public final native void put(String key, Object value)
	/*-{
    this[key] = value;
	}-*/;

	public final native String[] keySet()
	/*-{
    return Object.keys(this);
	}-*/;

	public final native String getString(String key)
	/*-{
    return this[key];
	}-*/;

	public final native void putString(String key, String value)
	/*-{
    this[key] = value;
	}-*/;

	public final native void remove(String key)
	/*-{
    delete this[key];
	}-*/;

	public static JsMapStringJsObject create(Map<String, JavaScriptObject> map) {
		JsMapStringJsObject ret = JavaScriptObject.createObject().cast();
		for (Map.Entry<String, JavaScriptObject> entry : map.entrySet()) {
			ret.put(entry.getKey(), entry.getValue());
		}
		return ret;
	}
}
