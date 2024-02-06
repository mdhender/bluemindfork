/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.dataprotect.sdsspool;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import net.bluemind.core.api.Stream;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.rest.base.JsonStreams;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.dataprotect.api.IBackupWorker;
import net.bluemind.dataprotect.api.IDPContext;
import net.bluemind.network.topology.Topology;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.sds.dto.DeleteRequest;
import net.bluemind.sds.dto.GetRequest;
import net.bluemind.sds.store.ISdsSyncStore;
import net.bluemind.sds.store.loader.SdsStoreLoader;
import net.bluemind.sds.sync.api.ISdsSync;
import net.bluemind.server.api.Server;
import net.bluemind.server.api.TagDescriptor;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.SystemConf;
import net.bluemind.system.helper.ArchiveHelper;
import net.bluemind.system.sysconf.helper.LocalSysconfCache;
import net.bluemind.utils.ProgressPrinter;

public class SdsSpoolWorker implements IBackupWorker {
	private final Path rootPath;
	private final SdsDataProtectSpool sdsSpool;
	private final Map<String, Optional<ISdsSyncStore>> sdsStores;
	private static final Logger logger = LoggerFactory.getLogger(SdsSpoolWorker.class);

	public SdsSpoolWorker() {
		rootPath = SdsDataProtectSpool.DEFAULT_PATH.getParent();
		sdsSpool = new SdsDataProtectSpool(SdsDataProtectSpool.DEFAULT_PATH);
		sdsStores = new HashMap<>();
		SystemConf config = LocalSysconfCache.get();
		for (ItemValue<Server> server : Topology.get().all(TagDescriptor.mail_imap.getTag())) {
			sdsStores.put(server.uid, new SdsStoreLoader().forSysconf(config, server.uid));
		}
	}

	@Override
	public boolean supportsTag(String tag) {
		return "bm/core".equals(tag);
	}

	@Override
	public String getDataType() {
		return "sds-spool";
	}

	@Override
	public void prepareDataDirs(IDPContext ctx, String tag, ItemValue<Server> toBackup) throws ServerFault {
		SystemConf sysconf = LocalSysconfCache.get();
		if (!ArchiveHelper.isSdsArchiveKind(sysconf)) {
			return;
		}
		if (sysconf.stringList(SysConfKeys.dataprotect_skip_datatypes.name()).contains("sds-spool")) {
			return;
		}

		INodeClient nc = NodeActivator.get(toBackup.value.address());
		nc.mkdirs(sdsSpool.path().toString());

		ISdsSync sdsSyncApi = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ISdsSync.class);

		AtomicLong lastIndex = new AtomicLong(readLastIndex(nc));
		Stream sdsSyncStream = sdsSyncApi.sync(lastIndex.get());
		ProgressPrinter progress = new ProgressPrinter(sdsSyncApi.count(lastIndex.get()));
		CompletableFuture<Void> future = JsonStreams.consume(VertxStream.read(sdsSyncStream), body -> {
			progress.add();
			String type = body.getString("type");
			if (type.equals("BODYADD") || type.equals("BODYDEL")) {
				String guid = body.getString("key");
				String serverUid = body.getString("srv");
				var optStore = sdsStores.getOrDefault(serverUid, Optional.empty());
				optStore.ifPresentOrElse(store -> {
					if (type.equals("BODYADD")) {
						Path fp = sdsSpool.livePath(guid);
						nc.mkdirs(fp.getParent().toString());
						var response = store.downloadRaw(GetRequest.of("", guid, fp.toString()));
						if (response.error != null) {
							logger.error("Unable to find guid {}@{}", guid, serverUid);
						}
					} else if (type.equals("BODYDEL")) {
						var response = store.delete(DeleteRequest.of(guid));
						if (response.error != null) {
							logger.error("Unable to delete guid {}@{}", guid, serverUid);
						}
					}
				}, () -> {
					throw new ServerFault("SdsStore not found for serverUid=" + serverUid);
				});
				if (progress.shouldPrint()) {
					logger.info("Progress: {}", progress);
				}
			}
			long index = body.getLong("index");
			lastIndex.accumulateAndGet(index, Math::max);
			if ((lastIndex.get() % 10000) == 0) {
				saveLastIndex(nc, lastIndex.get());
			}
		});
		try {
			future.orTimeout(23, TimeUnit.HOURS).join();
		} finally {
			saveLastIndex(nc, lastIndex.get());
		}
	}

	private void saveLastIndex(INodeClient nc, long lastIndex) {
		JsonObject jo = new JsonObject();
		jo.put("index", lastIndex);
		nc.writeFile(rootPath.resolve("stream-index.json").toString(),
				new ByteArrayInputStream(jo.toBuffer().getBytes()));
	}

	private long readLastIndex(INodeClient nc) {
		Path index = rootPath.resolve("stream-index.json");
		if (Files.notExists(index)) {
			return 0L;
		}
		String indexpath = index.toString();
		try {
			JsonObject jo = new JsonObject(Buffer.buffer(nc.read(indexpath)));
			return jo.getLong("index");
		} catch (Exception e) {
			logger.warn("Unable to read last index from {}: {}", indexpath, e.getMessage());
			return 0L;
		}
	}

	@Override
	public Set<String> getDataDirs() {
		return Set.of(rootPath.toString());
	}
}
