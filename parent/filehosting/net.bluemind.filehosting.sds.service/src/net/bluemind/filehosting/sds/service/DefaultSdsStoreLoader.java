/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.filehosting.sds.service;

import java.util.List;
import java.util.Optional;

import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.sds.store.ISdsBackingStoreFactory;
import net.bluemind.sds.store.ISdsSyncStore;
import net.bluemind.system.api.ArchiveKind;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.SystemConf;

public class DefaultSdsStoreLoader {

	private final List<ISdsBackingStoreFactory> stores;

	public DefaultSdsStoreLoader() {
		RunnableExtensionLoader<ISdsBackingStoreFactory> rel = new RunnableExtensionLoader<>();
		this.stores = rel.loadExtensions("net.bluemind.sds", "store", "store", "factory");
	}

	public Optional<ISdsSyncStore> forSysconf(SystemConf sysconf) {
		ArchiveKind archiveKind = ArchiveKind.fromName(sysconf.stringValue(SysConfKeys.archive_kind.name()));
		if (archiveKind == null) {
			return Optional.empty();
		}

		return stores.stream().filter(sbs -> sbs.kind() == archiveKind).findAny()
				.map(s -> s.createSync(VertxPlatform.getVertx(), sysconf));
	}

}
