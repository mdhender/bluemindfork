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
package net.bluemind.ui.adminconsole.base.orgunit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.bluemind.directory.api.OrgUnitPath;
import net.bluemind.directory.api.gwt.js.JsOrgUnitPath;

public class OUUtils {

	public static String toPath(OrgUnitPath path) {
		List<String> all = new ArrayList<>();
		for (OrgUnitPath p = path; p != null; p = p.parent) {
			all.add(p.name);
		}
		Collections.reverse(all);
		return String.join("/", all);
	}

	public static String toPath(JsOrgUnitPath path) {
		List<String> all = new ArrayList<>();
		for (JsOrgUnitPath p = path; p != null; p = p.getParent()) {
			all.add(p.getName());
		}
		Collections.reverse(all);
		return String.join("/", all);

	}

}
