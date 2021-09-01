/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.backend.mail.replica.service.sds;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Vertx;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.sds.dto.DeleteRequest;
import net.bluemind.sds.dto.ExistRequest;
import net.bluemind.sds.dto.GetRequest;
import net.bluemind.sds.dto.MgetRequest;
import net.bluemind.sds.dto.MgetRequest.Transfer;
import net.bluemind.sds.dto.SdsResponse;
import net.bluemind.sds.store.ISdsBackingStoreFactory;
import net.bluemind.sds.store.ISdsSyncStore;
import net.bluemind.sds.store.noop.NoopStoreFactory;
import net.bluemind.system.api.ArchiveKind;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.SystemConf;
import net.bluemind.system.sysconf.helper.LocalSysconfCache;

public class MessageBodyObjectStore {

	private static final Logger logger = LoggerFactory.getLogger(MessageBodyObjectStore.class);
	private final BmContext ctx;
	private final ISdsSyncStore objectStore;
	private static final ConcurrentHashMap<String, ISdsSyncStore> objectStoreClientCache = new ConcurrentHashMap<>();

	private static final Map<ArchiveKind, ISdsBackingStoreFactory> archiveKindToObjectStore = loadStores();

	public MessageBodyObjectStore(BmContext ctx) {
		this.ctx = ctx;
		if (logger.isDebugEnabled()) {
			logger.debug("Object store for {}", this.ctx);
		}

		SystemConf config = sharedSysConf();

		this.objectStore = loadReader(config);
		if (logger.isDebugEnabled()) {
			logger.debug("Reading with {}", objectStore);
		}
	}

	private SystemConf sharedSysConf() {
		return LocalSysconfCache.get();
	}

	private ISdsSyncStore loadReader(SystemConf sysconf) {
		ArchiveKind archiveKind = ArchiveKind.fromName(sysconf.stringValue(SysConfKeys.archive_kind.name()));
		Vertx vertx = VertxPlatform.getVertx();
		if (archiveKind == null) {
			return new NoopStoreFactory().createSync(vertx, sysconf);
		}
		String key = archiveKind.name() + "-" + sysconf.stringValue(SysConfKeys.sds_s3_bucket.name());
		return objectStoreClientCache.computeIfAbsent(key, k -> {
			ISdsSyncStore syncStore;
			ISdsBackingStoreFactory factory = archiveKindToObjectStore.get(archiveKind);
			if (factory != null) {
				syncStore = factory.createSync(vertx, sysconf);
			} else {
				logger.error("factory for archive_kind {} not found: using NoopStore", archiveKind);
				syncStore = new NoopStoreFactory().createSync(vertx, sysconf);
			}
			logger.info("returning store: {}", syncStore);
			return syncStore;
		});
	}

	private static Map<ArchiveKind, ISdsBackingStoreFactory> loadStores() {
		RunnableExtensionLoader<ISdsBackingStoreFactory> rel = new RunnableExtensionLoader<>();
		List<ISdsBackingStoreFactory> stores = rel.loadExtensions("net.bluemind.sds", "store", "store", "factory");
		logger.info("Found {} backing store(s)", stores.size());
		return stores.stream().collect(Collectors.toMap(ISdsBackingStoreFactory::kind, f -> f));
	}

	/**
	 * Returns a sub-collections containing only the GUIDs that our known in the
	 * object store
	 * 
	 * @param bodyGuid
	 * @return
	 */
	public Set<String> exist(Set<String> guids) {
		logger.debug("Checking {} with {}", guids, objectStore);
		return guids.stream().filter(guid -> objectStore.exists(ExistRequest.of(guid)).exists)
				.collect(Collectors.toSet());

	}

	public Path open(String guid) {
		logger.debug("Open {} with {}", guid, objectStore);
		Path target = null;
		try {
			target = Files.createTempFile(guid, ".s3");
		} catch (IOException e1) {
			throw new ServerFault(e1);
		}
		try {
			SdsResponse resp = objectStore.download(GetRequest.of("", guid, target.toString()));
			if (resp.succeeded()) {
				return target;
			} else {
				throw new ServerFault(resp.error.message);
			}
		} catch (Exception e) {
			throw new ServerFault(e);
		}
	}

	public Path[] mopen(String[] guids) {
		if (guids.length == 1) {
			return new Path[] { open(guids[0]) };
		}
		logger.debug("Open {} with {}", guids, objectStore);
		MgetRequest mgetReq = new MgetRequest();
		mgetReq.transfers = new ArrayList<>();
		ArrayList<Path> paths = new ArrayList<>();
		for (String guid : guids) {
			try {
				Path tempPath = Files.createTempFile(guid, ".s3");
				tempPath = tempPath.toAbsolutePath();
				paths.add(tempPath);
				mgetReq.transfers.add(Transfer.of(guid, tempPath.toString()));
			} catch (IOException e) {
				throw new ServerFault(e);
			}
		}

		try {
			objectStore.downloads(mgetReq);
		} catch (Exception e) {
			throw new ServerFault(e);
		}
		return paths.toArray(new Path[] {});
	}

	public void delete(List<String> guids) {
		guids.stream().forEach(guid -> {
			objectStore.delete(DeleteRequest.of(guid));
		});
	}
}
