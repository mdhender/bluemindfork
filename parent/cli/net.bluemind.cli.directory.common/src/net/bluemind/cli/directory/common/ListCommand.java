package net.bluemind.cli.directory.common;

/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2018
 *
 * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License)
 * or the CeCILL as published by CeCILL.info (version 2 of the License).
 *
 * There are special exceptions to the terms and conditions of the
 * licenses as they are applied to this program. See LICENSE.txt in
 * the directory of this program distribution.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
import java.util.List;

import org.vertx.java.core.json.JsonObject;

import net.bluemind.core.container.api.ContainerQuery;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.BaseContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;

public abstract class ListCommand extends SingleOrDomainOperation {

	@Override
	public void synchronousDirOperation(String domainUid, ItemValue<DirEntry> de) {
		IContainers containers = ctx.adminApi().instance(IContainers.class);
		ContainerQuery query = ContainerQuery.type(getContainerType());
		query.owner = de.uid;
		List<BaseContainerDescriptor> containerList = containers.allLight(query);

		for (BaseContainerDescriptor baseContainerDescriptor : containerList) {
			JsonObject containerJson = new JsonObject();
			containerJson.putString("owner", baseContainerDescriptor.owner);
			containerJson.putString("uid", baseContainerDescriptor.uid);
			containerJson.putString("name", baseContainerDescriptor.name);
			ctx.info(containerJson.toString());
		}
	}

	@Override
	public Kind[] getDirEntryKind() {
		return new Kind[] { Kind.DOMAIN, Kind.USER };
	}

	public abstract String getContainerType();
}
