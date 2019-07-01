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

import java.util.Collections;
import java.util.Set;

import net.bluemind.core.api.BMApi;

/**
 * {@link OrgUnit} search parameters
 */
@BMApi(version = "3")
public class OrgUnitQuery {
	public int from = 0;
	public int size = -1;
	public String query;

	/**
	 * Used to limit search on managableKinds (sic)
	 */
	public Set<BaseDirEntry.Kind> managableKinds = Collections.emptySet();
}
