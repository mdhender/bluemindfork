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
package net.bluemind.sds.store.loader;

import java.util.List;
import java.util.Optional;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.sds.store.ISdsBackingStoreFactory;
import net.bluemind.sds.store.ISdsSyncStore;
import net.bluemind.system.api.ArchiveKind;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.SystemConf;

public class SdsStoreLoader {

	private static final Cache<String, ISdsSyncStore> currentStore = Caffeine.newBuilder()
			.evictionListener((String k, ISdsSyncStore v, RemovalCause c) -> {
				if (v != null) {
					v.close();
				}
			}).build();

	private static final List<ISdsBackingStoreFactory> stores = loadStores();

	private static List<ISdsBackingStoreFactory> loadStores() {
		RunnableExtensionLoader<ISdsBackingStoreFactory> rel = new RunnableExtensionLoader<>();
		return rel.loadExtensions("net.bluemind.sds", "store", "store", "factory");
	}

	protected ISdsSyncStore createSync(ISdsBackingStoreFactory factory, Vertx vertx, SystemConf sysconf) {
		JsonObject jsonconf = new JsonObject()//
				.put("storeType", sysconf.stringValue(SysConfKeys.archive_kind.name()))//
				.put("endpoint", sysconf.stringValue(SysConfKeys.sds_s3_endpoint.name()))//
				.put("accessKey", sysconf.stringValue(SysConfKeys.sds_s3_access_key.name()))//
				.put("secretKey", sysconf.stringValue(SysConfKeys.sds_s3_secret_key.name()))//
				.put("region", sysconf.stringValue(SysConfKeys.sds_s3_region.name()))//
				.put("bucket", sysconf.stringValue(SysConfKeys.sds_s3_bucket.name()))//
				.put("insecure", sysconf.booleanValue(SysConfKeys.sds_s3_insecure.name(), false));
		return currentStore.get(jsonconf.encode(), k -> factory.syncStore(factory.create(vertx, jsonconf)));
	}

	public Optional<ISdsSyncStore> forSysconf(SystemConf sysconf) {
		String archiveKind = Optional.ofNullable(sysconf.stringValue(SysConfKeys.archive_kind.name())).orElse("cyrus");
		if (archiveKind.isBlank() || archiveKind.equalsIgnoreCase("none")) {
			archiveKind = "cyrus";
		}
		ArchiveKind storeType = ArchiveKind.fromName(archiveKind);
		if (storeType == null || !storeType.isSdsArchive()) {
			return Optional.empty();
		}
		return stores.stream().filter(sbs -> sbs.kind() == storeType).findAny()
				.map(s -> createSync(s, VertxPlatform.getVertx(), sysconf));
	}
}
