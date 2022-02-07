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

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
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

	protected ISdsSyncStore createSync(ISdsBackingStoreFactory factory, Vertx vertx, SystemConf sysconf) {
		JsonObject jsonconf = new JsonObject()//
				.put("storeType", sysconf.stringValue(SysConfKeys.sds_filehosting_storetype.name()))//
				.put("endpoint", sysconf.stringValue(SysConfKeys.sds_filehosting_endpoint.name()))//
				.put("accessKey", sysconf.stringValue(SysConfKeys.sds_filehosting_s3_access_key.name()))//
				.put("secretKey", sysconf.stringValue(SysConfKeys.sds_filehosting_s3_secret_key.name()))//
				.put("region", sysconf.stringValue(SysConfKeys.sds_filehosting_s3_region.name()))//
				.put("bucket", sysconf.stringValue(SysConfKeys.sds_filehosting_s3_bucket.name()));
		return factory.syncStore(factory.create(vertx, jsonconf));
	}

	public Optional<ISdsSyncStore> forSysconf(SystemConf sysconf) {
		ArchiveKind storeType = ArchiveKind.fromName(sysconf.stringValue(SysConfKeys.sds_filehosting_storetype.name()));
		if (storeType == null || !storeType.isSdsArchive()) {
			return Optional.empty();
		}
		return stores.stream().filter(sbs -> sbs.kind() == storeType).findAny()
				.map(s -> createSync(s, VertxPlatform.getVertx(), sysconf));
	}

}
