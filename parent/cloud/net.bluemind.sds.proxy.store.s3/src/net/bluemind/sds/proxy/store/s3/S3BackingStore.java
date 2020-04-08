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
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

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
import net.bluemind.sds.proxy.dto.MgetRequest;
import net.bluemind.sds.proxy.dto.MgetRequest.Transfer;
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
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

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
	private final Counter mgetRequestCounter;
	private final Timer mgetLatencyTimer;
	private Counter getFailureRequestCounter;

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
		getFailureRequestCounter = registry
				.counter(idFactory.name("request").withTag("method", "get").withTag("status", "error"));
		mgetLatencyTimer = registry.timer(idFactory.name("latency").withTag("method", "mget"));
		mgetRequestCounter = registry
				.counter(idFactory.name("request").withTag("method", "ùget").withTag("status", "success"));
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
						final long start = clock.monotonicTime();

						Path toUpload = Paths.get(req.filename);
						return client
								.putObject(PutObjectRequest.builder().bucket(bucket).key(req.guid).build(), toUpload)
								.exceptionally(ex -> null).thenApply(putResp -> {
									Optional<PutObjectResponse> optPut = Optional.ofNullable(putResp);
									SdsResponse sr = new SdsResponse();
									registry.timer(idFactory.name("latency").withTag("method", "put"))
											.record(clock.monotonicTime() - start, TimeUnit.NANOSECONDS);
									boolean success = optPut.map(p -> p.sdkHttpResponse().statusCode() == 200)
											.orElse(false);
									registry.counter(idFactory.name("transfer").withTag("direction", "upload"))
											.increment(toUpload.toFile().length());
									registry.counter(idFactory.name("request").withTag("method", "put")
											.withTag("status", success ? "success" : "error")).increment();
									if (success) {
										sr.withTags(ImmutableMap.of("guid", req.guid));
									} else {
										sr.error = new SdsError(
												optPut.map(p -> p.sdkHttpResponse().statusText().orElse("missing"))
														.orElse("no status"));
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
				.exceptionally(ex -> null).thenApply(gor -> {
					getLatencyTimer.record(clock.monotonicTime() - start, TimeUnit.NANOSECONDS);
					if (gor != null) {
						getSizeCounter.increment(target.length());
						getRequestCounter.increment();
					} else {
						getFailureRequestCounter.increment();
					}
					return SdsResponse.UNTAGGED_OK;
				});

	}

	@Override
	public CompletableFuture<SdsResponse> downloads(MgetRequest req) {
		final long start = clock.monotonicTime();

		Collection<Transfer> dedupByFilename = req.transfers.stream()
				.collect(Collectors.toMap(t -> t.filename, t -> t, (t1, t2) -> t2)).values();

		File[] files = dedupByFilename.stream().map(t -> {
			File f = new File(t.filename);
			f.delete();// NOSONAR
			return f;
		}).toArray(File[]::new);
		CompletableFuture<?>[] fut = new CompletableFuture[files.length];
		int i = 0;
		final LongAdder totalSize = new LongAdder();
		for (Transfer t : dedupByFilename) {
			final int slot = i;
			fut[slot] = client
					.getObject(GetObjectRequest.builder().bucket(bucket).key(t.guid).build(), files[slot].toPath())
					.exceptionally(x -> {
						logger.warn(x.getMessage());
						return null;
					}).thenAccept(gor -> totalSize.add(files[slot].length()));
			i++;
		}
		return CompletableFuture.allOf(fut).thenApply(v -> {
			mgetLatencyTimer.record(clock.monotonicTime() - start, TimeUnit.NANOSECONDS);
			getSizeCounter.increment(totalSize.longValue());
			mgetRequestCounter.increment();
			return new SdsResponse().withTags(ImmutableMap.of("batch", Integer.toString(files.length), "sizeKB",
					Long.toString(totalSize.longValue() / 1024)));
		}).exceptionally(ex -> {
			logger.error(ex.getMessage() + " for " + req, ex);
			SdsResponse error = new SdsResponse();
			error.error = new SdsError(ex.getMessage());
			return error;
		});
	}

	@Override
	public CompletableFuture<SdsResponse> delete(DeleteRequest req) {
		final long start = clock.monotonicTime();
		return client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(req.guid).build())
				.thenApply(dor -> {
					registry.timer(idFactory.name("latency").withTag("method", "delete"))
							.record(clock.monotonicTime() - start, TimeUnit.NANOSECONDS);
					registry.counter(idFactory.name("request").withTag("method", "delete").withTag("status", "success"))
							.increment();
					return SdsResponse.UNTAGGED_OK;
				});

	}

}
