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
package net.bluemind.ui.adminconsole.jobs;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.client.JsArrayString;

import net.bluemind.core.commons.gwt.JsMapStringString;

public class MapBuilder {

	public static Map<String, String> of(String... kv) {
		if (kv.length % 2 != 0) {
			throw new RuntimeException("invalid param count");
		}
		HashMap<String, String> ret = new HashMap<String, String>();
		for (int i = 0; i < kv.length; i += 2) {
			ret.put(kv[i], kv[i + 1]);
		}
		return ret;
	}

	public static Map<String, String> of(JsMapStringString state) {
		HashMap<String, String> ret = new HashMap<String, String>();
		JsArrayString keys = state.keys();
		for (int i = 0; i < keys.length(); i++) {
			String k = keys.get(i);
			String v = state.get(k);
			ret.put(k, v);
		}
		return ret;
	}

}
