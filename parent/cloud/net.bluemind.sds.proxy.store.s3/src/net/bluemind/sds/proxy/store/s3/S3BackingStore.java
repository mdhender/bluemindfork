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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.netflix.spectator.api.Clock;
import com.netflix.spectator.api.Counter;
import com.netflix.spectator.api.Registry;
import com.netflix.spectator.api.Timer;

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
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.BucketLocationConstraint;
import software.amazon.awssdk.services.s3.model.CreateBucketConfiguration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class S3BackingStore implements ISdsBackingStore {

	private static final Logger logger = LoggerFactory.getLogger(S3BackingStore.class);
	private final S3AsyncClient client;
	private final String bucket;

	private final Registry registry;
	private final IdFactory idFactory;
	private final Timer getLatencyTimer;
	private final Timer existLatencyTimer;
	private final Clock clock;
	private final Counter getSizeCounter;
	private final Counter getRequestCounter;

	public S3BackingStore(S3Configuration s3Configuration, Registry registry, IdFactory idfactory) {
		this.registry = registry;
		this.idFactory = idfactory;

		this.client = S3ClientFactory.create(s3Configuration);
		try {
			this.bucket = client.listBuckets()
					.thenApply(
							lbr -> lbr.buckets().stream().anyMatch(b -> b.name().equals(s3Configuration.getBucket())))
					.thenCompose(exist -> {
						if (exist.booleanValue()) {
							return CompletableFuture.completedFuture(s3Configuration.getBucket());
						} else {
							return client.createBucket(CreateBucketRequest.builder()//
									.createBucketConfiguration(CreateBucketConfiguration.builder()
											.locationConstraint(BucketLocationConstraint.EU).build())//
									.bucket(s3Configuration.getBucket()).build()).thenApply(cbr -> {
										if (cbr.sdkHttpResponse().isSuccessful()) {
											return s3Configuration.getBucket();
										} else {
											throw new SdsException(
													"bucket creation error: " + cbr.sdkHttpResponse().statusText());
										}
									});
						}
					}).get(10, TimeUnit.SECONDS);
			logger.info("Created {} for bucket {}", this, bucket);
		} catch (Exception e) {
			throw new SdsException(e);
		}
		clock = registry.clock();
		getLatencyTimer = registry.timer(idFactory.name("latency").withTag("method", "get"));
		getSizeCounter = registry.counter(idFactory.name("transfer").withTag("direction", "download"));
		getRequestCounter = registry
				.counter(idFactory.name("request").withTag("method", "get").withTag("status", "success"));
		existLatencyTimer = registry.timer(idFactory.name("latency").withTag("method", "exist"));
	}

	@Override
	public CompletableFuture<ExistResponse> exists(ExistRequest req) {
		final long start = clock.monotonicTime();
		return client.headObject(HeadObjectRequest.builder().bucket(bucket).key(req.guid).build())
				.exceptionally(r -> null).thenApply(head -> {
					boolean known = head != null && head.sdkHttpResponse().statusCode() == 200;
					existLatencyTimer.record(clock.monotonicTime() - start, TimeUnit.NANOSECONDS);
					registry.counter(idFactory.name("request").withTag("method", "exist").withTag("status",
							known ? "success" : "error")).increment();
					return ExistResponse.from(known);
				});
	}

	@Override
	public CompletableFuture<SdsResponse> upload(PutRequest req) {

		return client.headObject(HeadObjectRequest.builder().bucket(bucket).key(req.guid).build())
				.exceptionally(t -> null)//
				.thenCompose(head -> {
					if (head != null && head.sdkHttpResponse().statusCode() == 200) {
						SdsResponse sr = new SdsResponse();
						sr.withTags(ImmutableMap.of("guid", req.guid, "skip", "true"));
						return CompletableFuture.completedFuture(sr);
					} else {
						final long start = registry.clock().monotonicTime();

						Path toUpload = Paths.get(req.filename);
						return client
								.putObject(PutObjectRequest.builder().bucket(bucket).key(req.guid).build(), toUpload)
								.thenApply(put -> {
									SdsResponse sr = new SdsResponse();
									registry.timer(idFactory.name("latency").withTag("method", "put"))
											.record(registry.clock().monotonicTime() - start, TimeUnit.NANOSECONDS);
									boolean success = put.sdkHttpResponse().statusCode() == 200;
									registry.counter(idFactory.name("transfer").withTag("direction", "upload"))
											.increment(toUpload.toFile().length());
									registry.counter(idFactory.name("request").withTag("method", "put")
											.withTag("status", success ? "success" : "error")).increment();
									if (success) {
										sr.withTags(ImmutableMap.of("guid", req.guid));
									} else {
										sr.error = new SdsError(put.sdkHttpResponse().statusText().orElse("no status"));
									}
									return sr;
								});
					}
				});

	}

	@Override
	public CompletableFuture<SdsResponse> download(GetRequest req) {
		File target = new File(req.filename);
		final long start = clock.monotonicTime();
		target.delete(); // NOSONAR
		return client.getObject(GetObjectRequest.builder().bucket(bucket).key(req.guid).build(), target.toPath())
				.thenApply(gor -> {
					getLatencyTimer.record(clock.monotonicTime() - start, TimeUnit.NANOSECONDS);
					getSizeCounter.increment(target.length());
					getRequestCounter.increment();
					return SdsResponse.UNTAGGED_OK;
				});

	}

	@Override
	public CompletableFuture<SdsResponse> delete(DeleteRequest req) {
		SdsResponse sr = new SdsResponse();
		final long start = registry.clock().monotonicTime();
		return client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(req.guid).build())
				.thenApply(dor -> {
					registry.timer(idFactory.name("latency").withTag("method", "delete"))
							.record(registry.clock().monotonicTime() - start, TimeUnit.NANOSECONDS);
					registry.counter(idFactory.name("request").withTag("method", "delete").withTag("status", "success"))
							.increment();
					return sr;
				});

	}

}
