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
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.spectator.api.DistributionSummary;
import com.netflix.spectator.api.Registry;

import net.bluemind.backend.mail.replica.service.sds.IObjectStoreReader;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.metrics.registry.IdFactory;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;
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
		} catch (IOException e1) {
			throw new ServerFault(e1);
		}
		try {
			final Path notNull = target;
			return client.getObject(GetObjectRequest.builder().bucket(bucket).key(guid).build(),
					new SimpleDownloader<>(target)).thenApply(v -> {
						registry.distributionSummary(idFactory.name("read")).record(v.contentLength());
						return notNull;
					}).get(10, TimeUnit.SECONDS);
		} catch (Exception e) {
			throw new ServerFault(e);
		}
	}

	private static class SimpleDownloader<T> implements AsyncResponseTransformer<T, T> {

		private final Path path;
		private CompletableFuture<T> cf;
		private T response;
		private long transferred;
		private SeekableByteChannel channel;

		public SimpleDownloader(Path path) {
			this.path = path;
		}

		@Override
		public CompletableFuture<T> prepare() {
			cf = new CompletableFuture<>();
			try {
				this.channel = Files.newByteChannel(path, StandardOpenOption.CREATE,
						StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
			} catch (IOException e) {
				cf.completeExceptionally(e);
			}

			return cf.thenApply(v -> response);
		}

		@Override
		public void onResponse(T response) {
			this.response = response;
		}

		@Override
		public void onStream(SdkPublisher<ByteBuffer> publisher) {
			publisher.subscribe(new Subscriber<ByteBuffer>() {

				private Subscription sub;

				@Override
				public void onSubscribe(Subscription s) {
					sub = s;
					sub.request(1);
				}

				@Override
				public void onNext(ByteBuffer t) {
					transferred += t.remaining();
					try {
						channel.write(t);
						sub.request(1);
					} catch (IOException e) {
						exceptionOccurred(e);
					}
				}

				@Override
				public void onError(Throwable t) {
					exceptionOccurred(t);
				}

				@Override
				public void onComplete() {
					logger.debug("Completed of {} byte(s) transferred", transferred);
					try {
						channel.close();
						channel = null;
					} catch (IOException e) {
					}
					cf.complete(response);
				}
			});

		}

		@Override
		public void exceptionOccurred(Throwable error) {
			try {
				if (channel != null) {
					channel.close();
				}
				Files.deleteIfExists(path);
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			}
			cf.completeExceptionally(error);
		}

	}

	@Override
	public Path[] mread(String... guids) {
		Path[] ret = new Path[guids.length];
		for (int i = 0; i < guids.length; i++) {
			try {
				ret[i] = Files.createTempFile(guids[i], ".s3");
			} catch (IOException e1) {
				throw new ServerFault(e1);
			}
		}
		CompletableFuture<?>[] dls = new CompletableFuture[ret.length];
		DistributionSummary distSum = registry.distributionSummary(idFactory.name("read"));
		for (int i = 0; i < guids.length; i++) {
			final Path cur = ret[i];
			dls[i] = client.getObject(GetObjectRequest.builder().bucket(bucket).key(guids[i]).build(),
					new SimpleDownloader<>(cur)).thenAccept(v -> distSum.record(v.contentLength()));
		}
		try {
			CompletableFuture.allOf(dls).get(15, TimeUnit.SECONDS);
		} catch (Exception e) {
			throw new ServerFault(e);
		}
		return ret;
	}

}
