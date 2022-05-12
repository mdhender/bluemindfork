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
package net.bluemind.sds.store;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.sds.dto.DeleteRequest;
import net.bluemind.sds.dto.ExistRequest;
import net.bluemind.sds.dto.ExistResponse;
import net.bluemind.sds.dto.GetRequest;
import net.bluemind.sds.dto.MgetRequest;
import net.bluemind.sds.dto.PutRequest;
import net.bluemind.sds.dto.SdsResponse;
import net.bluemind.system.api.ArchiveKind;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.SystemConf;

public interface ISdsBackingStoreFactory {

	public ArchiveKind kind();

	public ISdsBackingStore create(Vertx vertx, JsonObject configuration);

	public default ISdsBackingStore create(Vertx vertx, SystemConf sysconf) {
		JsonObject jsonconf = new JsonObject()//
				.put("storeType", sysconf.stringValue(SysConfKeys.archive_kind.name()))//
				.put("endpoint", sysconf.stringValue(SysConfKeys.sds_s3_endpoint.name()))//
				.put("accessKey", sysconf.stringValue(SysConfKeys.sds_s3_access_key.name()))//
				.put("secretKey", sysconf.stringValue(SysConfKeys.sds_s3_secret_key.name()))//
				.put("region", sysconf.stringValue(SysConfKeys.sds_s3_region.name()))//
				.put("bucket", sysconf.stringValue(SysConfKeys.sds_s3_bucket.name()))//
				.put("insecure", sysconf.booleanValue(SysConfKeys.sds_s3_insecure.name(), false));
		return this.create(vertx, jsonconf);
	}

	default ISdsSyncStore createSync(Vertx vertx, SystemConf sysconf) {
		return syncStore(create(vertx, sysconf));
	}

	default ISdsSyncStore syncStore(ISdsBackingStore asyncStore) {
		return new ISdsSyncStore() {
			@Override
			public ExistResponse exists(ExistRequest req) {
				try {
					return asyncStore.exists(req).get(10, TimeUnit.SECONDS);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new ServerFault("exists got interrupted", ErrorCode.UNKNOWN);
				} catch (TimeoutException | ExecutionException e) {
					throw new ServerFault("exists failed or timed out", ErrorCode.TIMEOUT);
				}
			}

			@Override
			public SdsResponse upload(PutRequest req) {
				try {
					return asyncStore.upload(req).get(15, TimeUnit.SECONDS);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new ServerFault("upload got interrupted", ErrorCode.UNKNOWN);
				} catch (TimeoutException | ExecutionException e) {
					throw new ServerFault("upload failed or timed out", ErrorCode.TIMEOUT);
				}
			}

			@Override
			public SdsResponse download(GetRequest req) {
				try {
					return asyncStore.download(req).get(10, TimeUnit.SECONDS);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new ServerFault("download got interrupted", ErrorCode.UNKNOWN);
				} catch (TimeoutException | ExecutionException e) {
					throw new ServerFault("download failed or timed out", ErrorCode.TIMEOUT);
				}
			}

			@Override
			public SdsResponse downloads(MgetRequest req) {
				try {
					return asyncStore.downloads(req).get(15, TimeUnit.SECONDS);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new ServerFault("downloads got interrupted", ErrorCode.UNKNOWN);
				} catch (TimeoutException | ExecutionException e) {
					throw new ServerFault("downloads failed or timed out", ErrorCode.TIMEOUT);
				}
			}

			@Override
			public SdsResponse delete(DeleteRequest req) {
				try {
					return asyncStore.delete(req).get(10, TimeUnit.SECONDS);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new ServerFault("delete got interrupted", ErrorCode.UNKNOWN);
				} catch (TimeoutException | ExecutionException e) {
					throw new ServerFault("delete failed or timed out", ErrorCode.TIMEOUT);
				}
			}

			@Override
			public void close() {
				asyncStore.close();
			}
		};
	}

}
