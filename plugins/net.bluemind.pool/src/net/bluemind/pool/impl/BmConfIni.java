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
package net.bluemind.pool.impl;

import net.bluemind.utils.IniFile;

public class BmConfIni extends IniFile {

	private static final String iniPath = getIniPath();

	private static String getIniPath() {
		String ret = System.getProperty("net.bluemind.ini.path");
		if (ret == null) {
			ret = "/etc/bm/bm.ini";
		}
		return ret;
	}

	public BmConfIni() {
		super(iniPath);
	}

	@Override
	public String getCategory() {
		return "bm";
	}

	public String get(String string) {
		String value = getProperty(string);
		if (value != null) {
			return value.replace("\"", "");
		} else {
			return null;
		}
	}

}
