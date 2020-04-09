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
package net.bluemind.core.sds.replication;

import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.spectator.api.Registry;

import net.bluemind.aws.s3.utils.S3ClientFactory;
import net.bluemind.aws.s3.utils.S3Configuration;
import net.bluemind.backend.mail.replica.service.sds.IObjectStoreReader;
import net.bluemind.metrics.registry.IdFactory;
import net.bluemind.metrics.registry.MetricsRegistry;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.SystemConf;
import software.amazon.awssdk.services.s3.S3AsyncClient;

public class S3ObjectStoreReaderFactory implements IObjectStoreReader.Factory {

	private static final Logger logger = LoggerFactory.getLogger(S3ObjectStoreReader.class);
	private final ConcurrentHashMap<String, S3AsyncClient> s3ClientCache;

	private static final Registry registry = MetricsRegistry.get();
	private static final IdFactory idFactory = new IdFactory("replication.sds.s3", MetricsRegistry.get(),
			S3ObjectStoreReaderFactory.class);

	public S3ObjectStoreReaderFactory() {
		this.s3ClientCache = new ConcurrentHashMap<>();
	}

	@Override
	public String handledObjectStoreKind() {
		return "s3";
	}

	@Override
	public IObjectStoreReader create(SystemConf conf) {
		String endpoint = conf.stringValue(SysConfKeys.sds_s3_endpoint.name());
		String region = conf.stringValue(SysConfKeys.sds_s3_region.name());
		String accessKey = conf.stringValue(SysConfKeys.sds_s3_access_key.name());
		String secretKey = conf.stringValue(SysConfKeys.sds_s3_secret_key.name());
		String bucket = conf.stringValue(SysConfKeys.sds_s3_bucket.name());
		String cacheKey = String.join(";", endpoint, accessKey, secretKey, region);

		S3AsyncClient client = s3ClientCache.computeIfAbsent(cacheKey, key -> {
			logger.info("Creating S3 client for {}", endpoint);
			S3Configuration config = S3Configuration.withEndpointBucketKeys(endpoint, bucket, accessKey, secretKey,
					region);
			return S3ClientFactory.create(config);
		});

		return new S3ObjectStoreReader(client, bucket, registry, idFactory);
	}

}
