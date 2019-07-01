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
package net.bluemind.ui.gwtsharing.client;

import java.util.List;

import net.bluemind.core.container.model.acl.AccessControlEntry;

public class AclComparator {

	public static boolean aclEquals(List<AccessControlEntry> source, List<AccessControlEntry> target) {
		if (source.size() == target.size()) {
			for (int i = 0; i < source.size(); i++) {
				if (!aceEquals(source.get(i), target.get(i))) {
					return false;
				}
			}
			return true;
		} else {
			return false;
		}
	}

	public static boolean aceEquals(AccessControlEntry source, AccessControlEntry target) {
		return source.subject.equals(target.subject) && source.verb == target.verb;
	}

}
