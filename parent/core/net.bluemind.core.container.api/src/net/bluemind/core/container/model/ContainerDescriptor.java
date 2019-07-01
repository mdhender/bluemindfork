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
package net.bluemind.core.container.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.container.model.acl.Verb;

@BMApi(version = "3")
public class ContainerDescriptor extends BaseContainerDescriptor {

	@Deprecated
	public Boolean writable;
	public Set<Verb> verbs = Collections.emptySet();
	public boolean offlineSync;
	public long internalId;

	public static ContainerDescriptor create(String uid, String name, String owner, String type, String domainUid,
			boolean defaultContainer) {
		ContainerDescriptor ret = new ContainerDescriptor();
		ret.uid = uid;
		ret.name = name;
		ret.owner = owner;
		ret.type = type;
		ret.domainUid = domainUid;
		ret.defaultContainer = defaultContainer;
		ret.settings = new HashMap<String, String>();
		return ret;
	}

	public static ContainerDescriptor create(String uid, String name, String owner, String type, String domainUid,
			boolean defaultContainer, Map<String, String> settings) {
		ContainerDescriptor ret = new ContainerDescriptor();
		ret.uid = uid;
		ret.name = name;
		ret.owner = owner;
		ret.type = type;
		ret.domainUid = domainUid;
		ret.defaultContainer = defaultContainer;
		ret.settings = settings;
		return ret;
	}

	@Override
	public String toString() {
		return "ContainerDescriptor [uid=" + uid + ", name=" + name + ", owner=" + owner + ", type=" + type
				+ ", defaultContainer=" + defaultContainer + ", domainUid=" + domainUid + ", deleted=" + deleted + "]";
	}

}
