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

import java.util.Map;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class BaseContainerDescriptor {

	public String uid;
	public String name;
	public String owner;
	public String type;
	public boolean defaultContainer;
	public boolean readOnly;
	public String domainUid;
	public String ownerDisplayname;
	public String ownerDirEntryPath;
	public Map<String, String> settings;
	public boolean deleted;
	public String datalocation;

	public static BaseContainerDescriptor create(String uid, String name, String owner, String type, String domainUid,
			boolean defaultContainer) {
		BaseContainerDescriptor ret = new BaseContainerDescriptor();
		ret.uid = uid;
		ret.name = name;
		ret.owner = owner;
		ret.type = type;
		ret.domainUid = domainUid;
		ret.defaultContainer = defaultContainer;
		return ret;
	}

	@Override
	public String toString() {
		return "BaseContainerDescriptor [uid=" + uid + ", name=" + name + ", owner=" + owner + ", type=" + type
				+ ", defaultContainer=" + defaultContainer + ", readOnly=" + readOnly + ", domainUid=" + domainUid
				+ ", ownerDisplayname=" + ownerDisplayname + ", ownerDirEntryPath=" + ownerDirEntryPath + ", settings="
				+ settings + ", deleted=" + deleted + ", datalocation=" + datalocation + "]";
	}

}
