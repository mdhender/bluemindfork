/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2022
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

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;

import io.vertx.core.json.JsonObject;
import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.filehosting.api.IInternalBMFileSystem;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.sds.dto.PutRequest;
import net.bluemind.sds.store.ISdsBackingStoreFactory;
import net.bluemind.sds.store.ISdsSyncStore;
import net.bluemind.system.api.ArchiveKind;
import net.bluemind.system.api.SysConfKeys;

public class FileHostingMigrator {
	private final CliContext ctx;
	private final Map<String, String> conf;
	private final ISdsSyncStore store;
	private final int workers;

	public FileHostingMigrator(CliContext ctx, int workers, Map<String, String> conf) {
		this.ctx = ctx;
		this.conf = conf;
		this.workers = workers;
		this.store = getStore();
	}

	protected ISdsSyncStore getStore() {
		ArchiveKind storeType = ArchiveKind.fromName(conf.get(SysConfKeys.sds_filehosting_storetype.name()));
		if (storeType == null || !storeType.isSdsArchive()) {
			ctx.error(
					"Unable to migrate filehosting to SDS: sds_filehosting_storetype must be one of: [s3, scalityring]");
			throw new ServerFault("Incompatible sds_filehosting_storetype setting");
		}
		RunnableExtensionLoader<ISdsBackingStoreFactory> rel = new RunnableExtensionLoader<>();
		List<ISdsBackingStoreFactory> storeFactories = rel.loadExtensions("net.bluemind.sds", "store", "store",
				"factory");
		Optional<ISdsBackingStoreFactory> factory = storeFactories.stream().filter(sbs -> sbs.kind() == storeType)
				.findAny();

		JsonObject jsonconf = new JsonObject()//
				.put("storeType", conf.get(SysConfKeys.sds_filehosting_storetype.name()))//
				.put("endpoint", conf.get(SysConfKeys.sds_filehosting_endpoint.name()))//
				.put("accessKey", conf.get(SysConfKeys.sds_filehosting_s3_access_key.name()))//
				.put("secretKey", conf.get(SysConfKeys.sds_filehosting_s3_secret_key.name()))//
				.put("region", conf.get(SysConfKeys.sds_filehosting_s3_region.name()))//
				.put("bucket", conf.get(SysConfKeys.sds_filehosting_s3_bucket.name()));

		return factory.map(f -> f.syncStore(f.create(VertxPlatform.getVertx(), jsonconf, "not_a_valid_location")))
				.orElseThrow(() -> {
					ctx.error("Unable to get a factory for store type " + storeType.name());
					throw new ServerFault("Unable to get a factory for store type " + storeType.name());
				});
	}

	public void migrateFileHosting(Path rootPath) throws IOException {
		IInternalBMFileSystem service = ctx.adminApi().instance(IInternalBMFileSystem.class);
		migratePath(rootPath, Files::isRegularFile, filePath -> {
			JsonObject js = new JsonObject();
			js.put("path", filePath.toString());
			List<String> shareUidsByPath = service.getShareUidsByPath(filePath.toString());
			if (shareUidsByPath.isEmpty()) {
				return Arrays.asList("sds-" + Base64.getUrlEncoder().encodeToString(js.encode().getBytes()));
			} else {
				return shareUidsByPath;
			}
		});
	}

	private static String removeBinExtension(Path filePath) {
		String fname = filePath.toString();
		if (fname.endsWith(".bin")) {
			int pos = fname.lastIndexOf('.');
			if (pos > -1) {
				fname = fname.substring(0, pos);
			}
		}
		return fname;
	}

	public void migrateDocuments(Path rootPath) throws IOException {
		PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:*.bin");
		migratePath(rootPath, filePath -> {
			return Files.isRegularFile(filePath) && matcher.matches(filePath.getFileName());
		}, filePath -> Arrays.asList("doc-fs-" + removeBinExtension(filePath).replace('/', '_')));
	}

	public void migratePath(Path rootPath, Predicate<Path> filter, Function<Path, List<String>> getUid)
			throws IOException {
		ArrayBlockingQueue<Path> q = new ArrayBlockingQueue<>(workers);
		ExecutorService pool = Executors.newFixedThreadPool(workers);

		Files.walk(rootPath, FileVisitOption.FOLLOW_LINKS) //
				.filter(filter::test) //
				.forEach(p -> {
					Path relativePath = rootPath.relativize(p);
					List<String> uids = getUid.apply(relativePath);
					for (String uid : uids) {
						try {
							q.put(p); // block until a slot is free
						} catch (InterruptedException ie) {
						}
						pool.submit(() -> {
							try {
								ctx.info("{} -> {}", relativePath, uid);
								store.upload(PutRequest.of(uid, p.toAbsolutePath().toString()));
							} finally {
								q.remove(); // NOSONAR: We don't care what path we remove
							}
						});
					}
				});

		pool.shutdown();
		try {
			pool.awaitTermination(1, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
		}
	}

}
