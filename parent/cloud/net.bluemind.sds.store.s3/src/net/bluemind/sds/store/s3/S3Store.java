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
package net.bluemind.sds.store.s3;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.netflix.spectator.api.Clock;
import com.netflix.spectator.api.Counter;
import com.netflix.spectator.api.DistributionSummary;
import com.netflix.spectator.api.Registry;
import com.netflix.spectator.api.Timer;

import net.bluemind.aws.s3.utils.S3ClientFactory;
import net.bluemind.aws.s3.utils.S3Configuration;
import net.bluemind.metrics.registry.IdFactory;
import net.bluemind.sds.dto.DeleteRequest;
import net.bluemind.sds.dto.ExistRequest;
import net.bluemind.sds.dto.ExistResponse;
import net.bluemind.sds.dto.GetRequest;
import net.bluemind.sds.dto.MgetRequest;
import net.bluemind.sds.dto.MgetRequest.Transfer;
import net.bluemind.sds.dto.PutRequest;
import net.bluemind.sds.dto.SdsError;
import net.bluemind.sds.dto.SdsResponse;
import net.bluemind.sds.store.ISdsBackingStore;
import net.bluemind.sds.store.SdsException;
import net.bluemind.sds.store.s3.zstd.ZstdRequestBody;
import net.bluemind.sds.store.s3.zstd.ZstdResponseTransformer;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.BucketLocationConstraint;
import software.amazon.awssdk.services.s3.model.CreateBucketConfiguration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

public class S3Store implements ISdsBackingStore {

	private static final Logger logger = LoggerFactory.getLogger(S3Store.class);
	private final S3AsyncClient client;
	private final String bucket;

	private final IdFactory idFactory;
	private final Timer getLatencyTimer;
	private final Timer mgetLatencyTimer;
	private final Timer existLatencyTimer;
	private final Timer putLatencyTimer;
	private final Timer deleteLatencyTimer;
	private final Clock clock;
	private final Counter getSizeCounter;
	private final Counter getRequestCounter;
	private final Counter getFailureRequestCounter;
	private final Counter existRequestCounter;
	private final Counter existFailureRequestCounter;
	private final Counter putRequestCounter;
	private final Counter putFailureRequestCounter;
	private final Counter mgetRequestCounter;
	private final Counter deleteRequestCounter;
	private final Counter putSizeCounter;
	private DistributionSummary compressionRatio;

	public S3Store(S3Configuration s3Configuration, Registry registry, IdFactory idfactory) {
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
											.locationConstraint(constraint(s3Configuration)).build())//
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
		getSizeCounter = registry.counter(idFactory.name("transfer").withTag("direction", "download"));
		getRequestCounter = registry
				.counter(idFactory.name("request").withTag("method", "get").withTag("status", "success"));
		getFailureRequestCounter = registry
				.counter(idFactory.name("request").withTag("method", "get").withTag("status", "error"));
		existRequestCounter = registry
				.counter(idFactory.name("request").withTag("method", "exist").withTag("status", "success"));
		existFailureRequestCounter = registry
				.counter(idFactory.name("request").withTag("method", "exist").withTag("status", "error"));
		putRequestCounter = registry
				.counter(idFactory.name("request").withTag("method", "put").withTag("status", "success"));
		putFailureRequestCounter = registry
				.counter(idFactory.name("request").withTag("method", "put").withTag("status", "error"));
		mgetRequestCounter = registry
				.counter(idFactory.name("request").withTag("method", "mget").withTag("status", "success"));
		deleteRequestCounter = registry
				.counter(idFactory.name("request").withTag("method", "delete").withTag("status", "success"));
		putSizeCounter = registry.counter(idFactory.name("transfer").withTag("direction", "upload"));
		existLatencyTimer = registry.timer(idFactory.name("latency").withTag("method", "exist"));
		mgetLatencyTimer = registry.timer(idFactory.name("latency").withTag("method", "mget"));
		getLatencyTimer = registry.timer(idFactory.name("latency").withTag("method", "get"));
		putLatencyTimer = registry.timer(idFactory.name("latency").withTag("method", "put"));
		compressionRatio = registry.distributionSummary(idFactory.name("compressionRatio"));
		deleteLatencyTimer = registry.timer(idFactory.name("latency").withTag("method", "delete"));
	}

	private static BucketLocationConstraint constraint(S3Configuration conf) {
		return Optional.ofNullable(Strings.emptyToNull(conf.region)).map(BucketLocationConstraint::fromValue)
				.orElse(BucketLocationConstraint.EU);
	}

	@SuppressWarnings("serial")
	private static class S3StoreException extends RuntimeException {
		public S3StoreException(Throwable t) {
			super(t);
		}
	}

	private static final HeadObjectResponse HEAD_NOT_FOUND = (HeadObjectResponse) HeadObjectResponse.builder()
			.sdkHttpResponse(SdkHttpResponse.builder().statusCode(404).build()).build();

	@Override
	public CompletableFuture<ExistResponse> exists(ExistRequest req) {
		final long start = clock.monotonicTime();
		return client.headObject(HeadObjectRequest.builder().bucket(bucket).key(req.guid).build()).exceptionally(t -> {
			if (t.getCause() instanceof NoSuchKeyException) {
				return HEAD_NOT_FOUND;
			} else {
				throw new S3StoreException(t.getCause());
			}
		}).thenApply(head -> {
			boolean known = head != null && head.sdkHttpResponse().statusCode() == 200;
			existLatencyTimer.record(clock.monotonicTime() - start, TimeUnit.NANOSECONDS);
			Counter requestCounter = known ? existRequestCounter : existFailureRequestCounter;
			requestCounter.increment();
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
						return client.putObject(PutObjectRequest.builder().bucket(bucket).key(req.guid).build(),
								new ZstdRequestBody(toUpload, compressionRatio)).exceptionally(ex -> {
									logger.error("put {} failed: {}", req, ex.getMessage());
									return null;
								}).thenApply(putResp -> {
									Optional<PutObjectResponse> optPut = Optional.ofNullable(putResp);
									SdsResponse sr = new SdsResponse();
									putLatencyTimer.record(clock.monotonicTime() - start, TimeUnit.NANOSECONDS);
									boolean success = optPut.map(p -> p.sdkHttpResponse().statusCode() == 200)
											.orElse(false);
									putSizeCounter.increment(toUpload.toFile().length());
									Counter requestCounter = success ? putRequestCounter : putFailureRequestCounter;
									requestCounter.increment();
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

	private static final GetObjectResponse GET_NOT_FOUND = (GetObjectResponse) GetObjectResponse.builder()
			.sdkHttpResponse(SdkHttpResponse.builder().statusCode(404).build()).build();

	@Override
	public CompletableFuture<SdsResponse> download(GetRequest req) {
		final long start = clock.monotonicTime();
		ZstdResponseTransformer<GetObjectResponse> prt = new ZstdResponseTransformer<>(req.filename);
		return client.getObject(GetObjectRequest.builder().bucket(bucket).key(req.guid).build(), prt)
				.exceptionally(t -> {
					if (t.getCause() instanceof NoSuchKeyException) {
						logger.error("GET failed: {} not found", req.guid);
						return GET_NOT_FOUND;
					} else {
						throw new S3StoreException(t.getCause());
					}
				}).thenApply(gor -> {
					boolean notfound = gor != null && gor.sdkHttpResponse().statusCode() == 404;
					getLatencyTimer.record(clock.monotonicTime() - start, TimeUnit.NANOSECONDS);
					if (gor != null && !notfound) {
						getSizeCounter.increment(prt.transferred());
						getRequestCounter.increment();
						return SdsResponse.UNTAGGED_OK;
					} else {
						getFailureRequestCounter.increment();
						SdsResponse error = new SdsResponse();
						error.error = new SdsError("get " + req.guid + " failed");
						return error;
					}
				});
	}

	@Override
	public CompletableFuture<SdsResponse> downloads(MgetRequest req) {
		final long start = clock.monotonicTime();

		int len = req.transfers.size();
		final LongAdder totalSize = new LongAdder();
		int parallelStreams = 8;
		CompletableFuture<?>[] roots = new CompletableFuture[parallelStreams];
		for (int i = 0; i < parallelStreams; i++) {
			roots[i] = CompletableFuture.completedFuture(null);
		}
		Iterator<Transfer> it = req.transfers.iterator();
		for (int i = 0; i < len; i++) {
			int slot = i % parallelStreams;
			Transfer t = it.next();
			ZstdResponseTransformer<GetObjectResponse> pr = new ZstdResponseTransformer<>(t.filename);
			roots[slot] = roots[slot].thenCompose(
					v -> client.getObject(GetObjectRequest.builder().bucket(bucket).key(t.guid).build(), pr) //
							.exceptionally(ex -> {
								if (ex.getCause() instanceof NoSuchKeyException) {
									logger.error("GET failed: {} not found", t.guid);
								}
								throw new S3StoreException(ex.getCause());
							}) //
							.thenAccept(x -> totalSize.add(pr.transferred())));
		}

		return CompletableFuture.allOf(roots).thenApply(v -> {
			mgetLatencyTimer.record(clock.monotonicTime() - start, TimeUnit.NANOSECONDS);
			getSizeCounter.increment(totalSize.longValue());
			mgetRequestCounter.increment();
			String sizeKb = Long.toString(totalSize.longValue() / 1024);
			logger.debug("{} byte(s) downloaded from S3.", sizeKb);
			return new SdsResponse().withTags(ImmutableMap.of("batch", Integer.toString(len), "sizeKB", sizeKb));
		}).exceptionally(ex -> {
			logger.error(ex.getMessage() + " for " + req);
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
					deleteLatencyTimer.record(clock.monotonicTime() - start, TimeUnit.NANOSECONDS);
					deleteRequestCounter.increment();
					return SdsResponse.UNTAGGED_OK;
				});

	}

	@Override
	public void close() {
		if (client != null) {
			client.close();
		}
	}

}
