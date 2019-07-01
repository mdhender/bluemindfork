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
package net.bluemind.core.container.api;

import java.util.List;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.container.model.acl.Verb;

@BMApi(version = "3")
public class ContainerQuery {

	public String type;
	public String name;
	public List<Verb> verb;
	public String owner;
	public Boolean readonly;
	public int size = 0;

	public static ContainerQuery type(String type) {
		ContainerQuery ret = new ContainerQuery();
		ret.type = type;
		return ret;
	}

	public static ContainerQuery ownerAndType(String owner, String type) {
		ContainerQuery ret = new ContainerQuery();
		ret.type = type;
		ret.owner = owner;
		return ret;
	}

}
