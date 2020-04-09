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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.spectator.api.DistributionSummary;
import com.netflix.spectator.api.Registry;

import net.bluemind.backend.mail.replica.service.sds.IObjectStoreReader;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.metrics.registry.IdFactory;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;

public class S3ObjectStoreReader implements IObjectStoreReader {

	private static final Logger logger = LoggerFactory.getLogger(S3ObjectStoreReader.class);
	private final S3AsyncClient client;
	private final String bucket;
	private Registry registry;
	private IdFactory idFactory;

	public S3ObjectStoreReader(S3AsyncClient client, String bucket, Registry registry, IdFactory idFactory) {
		this.registry = registry;
		this.idFactory = idFactory;

		this.client = client;
		this.bucket = bucket;
	}

	@Override
	public boolean exist(String guid) {
		try {
			return client.headObject(HeadObjectRequest.builder().bucket(bucket).key(guid).build())
					.thenApply(hor -> hor.sdkHttpResponse().statusCode() == 200).get(10, TimeUnit.SECONDS);
		} catch (Exception e) {
			throw new ServerFault(e);
		}

	}

	@Override
	public Path read(String guid) {
		logger.info("Reader {}/{}", bucket, guid);
		Path target = null;
		try {
			target = Files.createTempFile(guid, ".s3");
			Files.delete(target);
		} catch (IOException e1) {
			throw new ServerFault(e1);
		}
		try {
			final Path notNull = target;
			return client.getObject(GetObjectRequest.builder().bucket(bucket).key(guid).build(), target)
					.thenApply(v -> {
						registry.distributionSummary(idFactory.name("read")).record(notNull.toFile().length());
						return notNull;
					}).get(10, TimeUnit.SECONDS);
		} catch (Exception e) {
			throw new ServerFault(e);
		}
	}

	@Override
	public Path[] mread(String... guids) {
		Path[] ret = new Path[guids.length];
		for (int i = 0; i < guids.length; i++) {
			try {
				ret[i] = Files.createTempFile(guids[i], ".s3");
				Files.delete(ret[i]);
			} catch (IOException e1) {
				throw new ServerFault(e1);
			}
		}
		CompletableFuture<?>[] dls = new CompletableFuture[ret.length];
		DistributionSummary distSum = registry.distributionSummary(idFactory.name("read"));
		for (int i = 0; i < guids.length; i++) {
			final Path cur = ret[i];
			dls[i] = client.getObject(GetObjectRequest.builder().bucket(bucket).key(guids[i]).build(), cur)
					.thenAccept(v -> {
						if (Files.exists(cur)) {
							distSum.record(cur.toFile().length());
						}
					});
		}
		try {
			CompletableFuture.allOf(dls).get(15, TimeUnit.SECONDS);
		} catch (Exception e) {
			throw new ServerFault(e);
		}
		return ret;
	}

}
