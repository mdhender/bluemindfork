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
package net.bluemind.directory.api;

import javax.validation.constraints.Pattern;

import net.bluemind.core.api.BMApi;

/**
 * BlueMind integrates a delegated administration functionality. It allows you
 * to grant limited administration rights to administrators (who become
 * delegated administrators). Delegated administration rights can be given to
 * specific users selected according to certain criteria (job type, industry,
 * geographical area...). The resulting group is called an {@link OrgUnit}
 */
@BMApi(version = "3")
public class OrgUnit {

	/**
	 * {@link OrgUnit} name
	 */
	@Pattern(regexp = "^[a-zA-Z0-9_ -]*$")
	public String name;

	/**
	 * Parent {@link OrgUnit} UID
	 */
	public String parentUid;

	public static OrgUnit create(String name, String parent) {
		OrgUnit ret = new OrgUnit();
		ret.name = name;
		ret.parentUid = parent;
		return ret;
	}
}
