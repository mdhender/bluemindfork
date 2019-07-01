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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.bluemind.core.api.BMApi;

/**
 * Delegation trees can be created and organized so as to allow different levels
 * of delegated administration. The {@link OrgUnitPath} represents an element of
 * the delegation tree
 */
@BMApi(version = "3")
public class OrgUnitPath {

	/**
	 * Element UID
	 */
	public String uid;
	/**
	 * Element name
	 */
	public String name;
	/**
	 * The parent {@link OrgUnitPath} element
	 */
	public OrgUnitPath parent;

	public static OrgUnitPath create(String uid) {
		return create(uid, null, null);
	}

	public static OrgUnitPath create(String uid, String name) {
		return create(uid, name, null);
	}

	public static OrgUnitPath create(String uid, String name, OrgUnitPath parent) {
		OrgUnitPath path = new OrgUnitPath();
		path.uid = uid;
		path.name = name;
		path.parent = parent;
		return path;
	}

	public List<String> path() {
		ArrayList<String> ret = new ArrayList<>();
		for (OrgUnitPath current = this; current != null; current = current.parent) {
			ret.add(current.uid);
		}
		return ret;
	}

	@Override
	public String toString() {
		ArrayList<String> ret = new ArrayList<>();
		for (OrgUnitPath current = this; current != null; current = current.parent) {
			ret.add(current.name);
		}
		Collections.reverse(ret);
		return String.join("/", ret.toArray(new String[0]));
	}

}
