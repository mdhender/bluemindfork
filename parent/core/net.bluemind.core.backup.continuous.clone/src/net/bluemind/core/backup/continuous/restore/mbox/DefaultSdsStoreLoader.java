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
package net.bluemind.core.backup.continuous.restore.mbox;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Vertx;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.network.topology.Topology;
import net.bluemind.sds.store.ISdsBackingStoreFactory;
import net.bluemind.sds.store.ISdsSyncStore;
import net.bluemind.sds.store.noop.NoopStoreFactory;
import net.bluemind.server.api.TagDescriptor;
import net.bluemind.system.api.ArchiveKind;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.SystemConf;

public class DefaultSdsStoreLoader implements ISdsStoreLoader {

	private final List<ISdsBackingStoreFactory> stores;
	private static final String FAKE_LOC = "fake_loc";
	private static final Logger logger = LoggerFactory.getLogger(DefaultSdsStoreLoader.class);

	public DefaultSdsStoreLoader() {
		RunnableExtensionLoader<ISdsBackingStoreFactory> rel = new RunnableExtensionLoader<>();
		this.stores = rel.loadExtensions("net.bluemind.sds", "store", "store", "factory");
	}

	public ISdsSyncStore forSysconf(SystemConf sysconf) {
		ArchiveKind archiveKind = ArchiveKind.fromName(sysconf.stringValue(SysConfKeys.archive_kind.name()));
		Vertx vertx = VertxPlatform.getVertx();
		if (archiveKind == null) {
			return new NoopStoreFactory().createSync(vertx, sysconf, FAKE_LOC);
		}
		String locToUse = FAKE_LOC;
		if (archiveKind.isShardedByDatalocation()) {
			if (Topology.get().singleNode()) {
				logger.warn("ArchiveKind {} is sharded by datalocation. A 'real' SDS store is recommended for cloning.",
						archiveKind);
				locToUse = Topology.get().any(TagDescriptor.bm_core.getTag()).uid;
			} else {
				throw new ServerFault("ArchiveKind " + archiveKind
						+ " is sharded by datalocation. A 'real' SDS store is needed for cloning.");
			}
		}
		final String resolvedLoc = locToUse;

		Optional<ISdsBackingStoreFactory> optFactory = stores.stream().filter(sbs -> sbs.kind() == archiveKind)
				.findAny();

		return optFactory.map(s -> s.createSync(vertx, sysconf, resolvedLoc))
				.orElseGet(() -> new NoopStoreFactory().createSync(vertx, sysconf, resolvedLoc));
	}

}
