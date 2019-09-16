/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.backend.cyrus.partitions;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import com.google.common.base.MoreObjects;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.server.api.Server;

public class CyrusPartition {

	public final String serverUid;
	public final String domainUid;
	public final String name;

	private CyrusPartition(String srv, String dom) {
		Objects.requireNonNull(srv, "serverUid/datalocation must not be null");
		Objects.requireNonNull(dom, "domainUid must not be null");
		this.serverUid = srv;
		this.domainUid = dom;
		this.name = serverUid.replace('.', '_') + "__" + domainUid.replace('.', '_');
	}

	public Path mainParent() {
		return Paths.get(CyrusFileSystemPathHelper.MAIN_ROOT + name);
	}

	public Path archiveParent() {
		return Paths.get(CyrusFileSystemPathHelper.ARCHIVE_ROOT + name);
	}

	public String toString() {
		return MoreObjects.toStringHelper(CyrusPartition.class)//
				.add("datalocation", serverUid)//
				.add("domain", domainUid)//
				.toString();
	}

	public static CyrusPartition forServerAndDomain(String serverUid, String domainUid) {
		return new CyrusPartition(serverUid, domainUid);
	}

	public static CyrusPartition forServerAndDomain(ItemValue<Server> server, String domainUid) {
		return new CyrusPartition(server.uid, domainUid);
	}

	public static CyrusPartition forName(String name) {
		int splitIdx = name.indexOf("__");
		if (splitIdx > 0) {
			return forServerAndDomain(name.substring(0, splitIdx).replace('_', '.'),
					name.substring(splitIdx + 2).replace('_', '.'));
		} else {
			return forServerAndDomain("unknown", name.replace('_', '.'));
		}
	}
}
