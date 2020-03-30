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
package net.bluemind.sds.proxy.store.s3;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteStreams;
import com.netflix.spectator.api.Registry;

import net.bluemind.aws.s3.utils.S3ClientFactory;
import net.bluemind.aws.s3.utils.S3Configuration;
import net.bluemind.metrics.registry.IdFactory;
import net.bluemind.sds.proxy.dto.DeleteRequest;
import net.bluemind.sds.proxy.dto.ExistRequest;
import net.bluemind.sds.proxy.dto.ExistResponse;
import net.bluemind.sds.proxy.dto.GetRequest;
import net.bluemind.sds.proxy.dto.PutRequest;
import net.bluemind.sds.proxy.dto.SdsError;
import net.bluemind.sds.proxy.dto.SdsResponse;
import net.bluemind.sds.proxy.store.ISdsBackingStore;
import net.bluemind.sds.proxy.store.SdsException;

public class S3BackingStore implements ISdsBackingStore {

	private static final Logger logger = LoggerFactory.getLogger(S3BackingStore.class);
	private final AmazonS3 client;
	private final Bucket bucket;

	private final Registry registry;
	private final IdFactory idFactory;

	public S3BackingStore(S3Configuration s3Configuration, Registry registry, IdFactory idfactory) {
		this.registry = registry;
		this.idFactory = idfactory;

		this.client = S3ClientFactory.create(s3Configuration);
		boolean exists = client.doesBucketExistV2(s3Configuration.getBucket());
		if (!exists) {
			logger.warn("Bucket {} does not exist", s3Configuration.getBucket());
			this.bucket = client.createBucket(s3Configuration.getBucket());
		} else {
			this.bucket = client.listBuckets().stream().filter(b -> b.getName().equals(s3Configuration.getBucket()))
					.findFirst().orElseThrow(() -> new SdsException(s3Configuration.getBucket() + " does not exist"));
		}
		logger.info("Created {} for bucket {}", this, bucket);
	}

	@Override
	public ExistResponse exists(ExistRequest req) {
		final long start = registry.clock().monotonicTime();
		boolean known = client.doesObjectExist(bucket.getName(), req.guid);
		registry.timer(idFactory.name("latency").withTag("method", "exist")).record(
			registry.clock().monotonicTime() - start, TimeUnit.NANOSECONDS);
		registry.counter(idFactory.name("request").withTag("method", "exist")
			.withTag("status", known ? "success" : "error")).increment();
		return ExistResponse.from(known);
	}

	@Override
	public SdsResponse upload(PutRequest req) throws IOException {
		SdsResponse sr = new SdsResponse();
		if (client.doesObjectExist(bucket.getName(), req.guid)) {
			sr.withTags(ImmutableMap.of("guid", req.guid, "skip", "true"));
			return sr;
		}
		try {
			File file = new File(req.filename);
			final long start = registry.clock().monotonicTime();
			PutObjectResult result = client.putObject(bucket.getName(), req.guid, file);
			registry.timer(idFactory.name("latency").withTag("method", "put")).record(
				registry.clock().monotonicTime() - start, TimeUnit.NANOSECONDS);
			sr.withTags(ImmutableMap.of("guid", req.guid));
			registry.counter(idFactory.name("transfer").withTag("direction", "upload")).increment(file.length());
			registry.counter(idFactory.name("request").withTag("method", "put")
				.withTag("status", "success"))
				.increment();
			logger.debug("Result {} {}", result.getETag(), result.getVersionId());
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			sr.error = new SdsError(e.getMessage());
			registry.counter(idFactory.name("request").withTag("method", "put")
				.withTag("status", "error")).increment();
		}
		return sr;
	}

	@Override
	public SdsResponse download(GetRequest req) throws IOException {
		SdsResponse sr = new SdsResponse();
		File target = new File(req.filename);
		final long start = registry.clock().monotonicTime();

		try (S3Object s3object = client.getObject(bucket.getName(), req.guid);
				S3ObjectInputStream stream = s3object.getObjectContent();
				OutputStream out = java.nio.file.Files.newOutputStream(target.toPath())) {
			ByteStreams.copy(stream, out);
			registry.timer(idFactory.name("latency").withTag("method", "get")).record(
				registry.clock().monotonicTime() - start, TimeUnit.NANOSECONDS);
			registry.counter(idFactory.name("transfer").withTag("direction", "download")).increment(target.length());
			registry.counter(idFactory.name("request").withTag("method", "get")
				.withTag("status", "success")).increment();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			sr.error = new SdsError(e.getMessage());
			registry.counter(idFactory.name("request").withTag("method", "get")
				.withTag("status", "error")).increment();
		}
		return sr;

	}

	@Override
	public SdsResponse delete(DeleteRequest req) {
		SdsResponse sr = new SdsResponse();
		try {
			registry.timer(idFactory.name("latency").withTag("method", "delete")).record(() -> {
				client.deleteObject(bucket.getName(), req.guid);
			});
			registry.counter(idFactory.name("request").withTag("method", "delete")
				.withTag("status", "success")).increment();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			sr.error = new SdsError(e.getMessage());
			registry.counter(idFactory.name("request_error").withTag("method", "delete")
				.withTag("status", "error")).increment();
		}
		return sr;
	}

}
