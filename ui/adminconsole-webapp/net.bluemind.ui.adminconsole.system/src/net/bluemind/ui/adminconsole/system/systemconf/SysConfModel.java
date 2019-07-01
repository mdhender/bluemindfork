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
package net.bluemind.ui.adminconsole.system.systemconf;

import com.google.gwt.core.client.JavaScriptObject;

import net.bluemind.core.commons.gwt.JsMapStringJsObject;

public final class SysConfModel extends JavaScriptObject {

	protected SysConfModel() {
	}

	public static SysConfModel from(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();
		JavaScriptObject sysConf = map.get("sysConf");
		if (sysConf == null) {
			sysConf = JavaScriptObject.createObject();
			map.put("sysConf", sysConf);
		}
		return sysConf.cast();
	}

	public final String get(String key) {
		return getValue(key);
	}

	public final void putString(String key, String value) {
		setValue(key, value);
	}

	public final native String[] keys()
	/*-{
    var ret = [];
    for (x in this) {
      ret.push(x);
    }
    return ret;
	}-*/;

	public final native String getValue(String key)
	/*-{
    return this[key];
	}-*/;

	public final native void setValue(String key, String value)
	/*-{
    this[key] = value;
	}-*/;
}