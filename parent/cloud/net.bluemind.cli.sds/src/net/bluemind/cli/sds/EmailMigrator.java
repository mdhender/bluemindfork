/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2024
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
package net.bluemind.cli.sds;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;

import io.netty.util.concurrent.DefaultThreadFactory;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.ReadStream;
import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.utils.JsonStreams;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.network.topology.Topology;
import net.bluemind.sds.dto.DeleteRequest;
import net.bluemind.sds.dto.GetRequest;
import net.bluemind.sds.dto.PutRequest;
import net.bluemind.sds.dto.SdsResponse;
import net.bluemind.sds.store.ISdsBackingStoreFactory;
import net.bluemind.sds.store.ISdsSyncStore;
import net.bluemind.sds.store.loader.SdsStoreLoader;
import net.bluemind.sds.sync.api.ISdsSync;
import net.bluemind.server.api.Server;
import net.bluemind.server.api.TagDescriptor;
import net.bluemind.system.api.ArchiveKind;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.SystemConf;
import net.bluemind.utils.ProgressPrinter;

public class EmailMigrator {
	private final CliContext ctx;
	private final ISdsSyncStore store;
	Map<String, Optional<ISdsSyncStore>> originStores;
	private final int workers;
	private Path root;
	private DB db;
	private HTreeMap<String, Long> migrationMap;

	public EmailMigrator(CliContext ctx, int workers, Path root, Map<String, String> conf) {
		this.ctx = ctx;
		this.workers = workers;
		this.store = getStore(conf);
		this.originStores = loadAllStores();
		this.root = root;
		loaddb();
	}

	private Map<String, Optional<ISdsSyncStore>> loadAllStores() {
		Map<String, Optional<ISdsSyncStore>> sdsStores = new HashMap<>();
		ISystemConfiguration configurationApi = ctx.adminApi().instance(ISystemConfiguration.class);
		SystemConf sysConf = configurationApi.getValues();
		for (ItemValue<Server> server : Topology.get().all(TagDescriptor.mail_imap.getTag())) {
			sdsStores.put(server.uid, new SdsStoreLoader().forSysconf(sysConf, server.uid));
		}
		return sdsStores;
	}

	private void loaddb() {
		try {
			db = DBMaker.fileDB(root.resolve("sds-migrate-v5.db").toAbsolutePath().toString()).transactionEnable()
					.fileMmapEnable().make();
			migrationMap = db.hashMap("migrate").keySerializer(Serializer.STRING_ASCII).valueSerializer(Serializer.LONG)
					.createOrOpen();
		} catch (Exception e) {
			ctx.error("Unable to open sds-migrate database: {}", e);
			System.exit(1);
		}
	}

	protected ISdsSyncStore getStore(Map<String, String> conf) {
		ArchiveKind storeType = ArchiveKind.fromName(conf.get(SysConfKeys.archive_kind.name()));
		if (storeType == null || !storeType.isSdsArchive()) {
			ctx.error("Unable to migrate filehosting to SDS: sds_archive_kind must be one of: [s3, scalityring]");
			throw new ServerFault("Incompatible sdsarchive_kind setting");
		}
		RunnableExtensionLoader<ISdsBackingStoreFactory> rel = new RunnableExtensionLoader<>();
		List<ISdsBackingStoreFactory> storeFactories = rel.loadExtensions("net.bluemind.sds", "store", "store",
				"factory");
		Optional<ISdsBackingStoreFactory> factory = storeFactories.stream().filter(sbs -> sbs.kind() == storeType)
				.findAny();

		JsonObject jsonconf = new JsonObject()//
				.put("storeType", conf.get(SysConfKeys.archive_kind.name()))//
				.put("endpoint", conf.get(SysConfKeys.sds_s3_endpoint.name()))//
				.put("accessKey", conf.get(SysConfKeys.sds_s3_access_key.name()))//
				.put("secretKey", conf.get(SysConfKeys.sds_s3_secret_key.name()))//
				.put("region", conf.get(SysConfKeys.sds_s3_region.name()))//
				.put("bucket", conf.get(SysConfKeys.sds_s3_bucket.name()))//
				.put("insecure", Boolean.getBoolean(conf.get(SysConfKeys.sds_s3_insecure.name())));

		return factory.map(f -> f.syncStore(f.create(VertxPlatform.getVertx(), jsonconf, "not_a_valid_location")))
				.orElseThrow(() -> {
					ctx.error("Unable to get a factory for store type " + storeType.name());
					throw new ServerFault("Unable to get a factory for store type " + storeType.name());
				});
	}

	public void clearCache() {
		migrationMap.clear();
		db.commit();
	}

	public void migrateEmails() {
		ISdsSync sdsSyncApi = ctx.infiniteRequestTimeoutAdminApi().instance(ISdsSync.class);
		AtomicLong lastIndex = new AtomicLong(migrationMap.getOrDefault("LASTINDEX", 0L));

		ProgressPrinter progress = new ProgressPrinter(sdsSyncApi.count(lastIndex.get()));
		ctx.info("to synchronize: {}", progress);

		ArrayBlockingQueue<String> q = new ArrayBlockingQueue<>(workers);
		ReadStream<Buffer> reader = VertxStream.read(sdsSyncApi.sync(lastIndex.get()));
		reader.pause();

		try (ExecutorService pool = Executors.newFixedThreadPool(workers, new DefaultThreadFactory("cli-sds-email"))) {
			CompletableFuture<Void> future = new JsonStreams(ctx).consume(reader, body -> {
				progress.add();
				String type = body.getString("type");
				if (!type.equals("BODYADD") && !type.equals("BODYDEL")) {
					return;
				}

				try {
					q.put(body.getString("key")); // block until a slot is free
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
				}

				pool.execute(() -> {
					String guid = body.getString("key");
					String serverUid = body.getString("srv");
					long index = body.getLong("index");
					lastIndex.accumulateAndGet(index, Math::max);
					q.remove(guid); // NOSONAR

					if (type.equals("BODYADD")) {
						upload(guid, serverUid);
					} else if (type.equals("BODYDEL")) {
						remove(guid, serverUid);
					}

					if ((lastIndex.get() % 1000) == 0) {
						saveLastIndex(lastIndex.get());
					}
					if (progress.shouldPrint()) {
						ctx.info("progress: {}", progress);
					}
				});

			});
			try {
				future.orTimeout(16, TimeUnit.DAYS).join();
			} catch (Exception e) {
				ctx.error("unknown error {}", e.getMessage());
			} finally {
				saveLastIndex(lastIndex.get());
			}
		}
	}

	private void remove(String guid, String serverUid) {
		store.delete(DeleteRequest.of(guid));
		migrationMap.remove(serverUid + "|" + guid);
	}

	private void upload(String guid, String serverUid) {
		originStores.getOrDefault(serverUid, Optional.empty()).ifPresentOrElse(originStore -> {
			File tempDownload;
			String dbKey = serverUid + "|" + guid;
			try {
				tempDownload = File.createTempFile("sds-" + guid, null, new File("/tmp"));
			} catch (IOException e) {
				migrationMap.put(dbKey, 1L);
				ctx.error("Unable to create temporary file: {}", e);
				db.commit();
				return;
			}
			try {
				SdsResponse response = originStore.downloadRaw(GetRequest.of("", guid, tempDownload.toString()));
				if (response.error != null) {
					ctx.error("Unable to find guid {}@{}", guid, serverUid);
					migrationMap.put(dbKey, 2L);
					db.commit();
					return;
				}
				SdsResponse putResponse = store.upload(PutRequest.of(guid, tempDownload.toString()));
				if (!putResponse.succeeded()) {
					ctx.error("put {} to {} failed: {}", guid, store, putResponse.error);
					migrationMap.put(dbKey, 3L);
					db.commit();
				} else {
					migrationMap.remove(dbKey);
				}
			} finally {
				tempDownload.delete(); // NOSONAR
			}
		}, () -> {
			throw new ServerFault("SdsStore not found for serverUid=" + serverUid);
		});
	}

	public void retryFailures() {
		migrationMap.forEach((serverUidGuid, failure) -> {
			// Ok ok, that's not very ajax'ed
			if (serverUidGuid.equals("LASTINDEX")) {
				return;
			}
			String[] splitted = serverUidGuid.split("|", 2);
			upload(splitted[1], splitted[0]);
		});
	}

	public long countFailures() {
		// -1 because LASTINDEX
		return Math.max(0L, migrationMap.sizeLong() - 1);
	}

	private void saveLastIndex(long index) {
		migrationMap.put("LASTINDEX", index);
		db.commit();
	}
}
