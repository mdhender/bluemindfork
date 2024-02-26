/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.sds.store.s3.zstd;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.luben.zstd.RecyclingBufferPool;
import com.github.luben.zstd.ZstdInputStream;

import io.netty.util.concurrent.DefaultThreadFactory;
import net.bluemind.common.io.Buffered;
import net.bluemind.sds.store.s3.IResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;

public class ZstdResponseTransformer<T> implements IResponseTransformer<T> {

	private static final Logger logger = LoggerFactory.getLogger(ZstdResponseTransformer.class);

	private final Path path;
	private CompletableFuture<T> cf;
	private T response;
	long transferred;

	public ZstdResponseTransformer(String path) {
		this.path = Paths.get(path);
	}

	@Override
	public CompletableFuture<T> prepare() {
		cf = new CompletableFuture<>();
		return cf.thenApply(v -> response);
	}

	@Override
	public void onResponse(T response) {
		this.response = response;
	}

	private static final ExecutorService decompThreads = Executors
			.newCachedThreadPool(new DefaultThreadFactory("zstd-processing"));

	@Override
	public void onStream(SdkPublisher<ByteBuffer> publisher) {

		try {

			publisher.subscribe(new Subscriber<ByteBuffer>() {

				private Subscription sub;
				private PipedInputStream toDecomp;
				private PipedOutputStream fromS3;
				private Future<?> future;

				@Override
				public void onSubscribe(Subscription s) {
					sub = s;

					// setup the decompression pipe
					try {
						toDecomp = new PipedInputStream(Buffered.writeBuffer());
						fromS3 = new PipedOutputStream();
						toDecomp.connect(fromS3);
					} catch (IOException e) {
						cf.completeExceptionally(e);
					}

					Runnable decompProcess = () -> {

						try (OutputStream os = Files.newOutputStream(path, StandardOpenOption.CREATE,
								StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
								BufferedOutputStream wb = Buffered.output(os);
								ZstdInputStream decomp = new ZstdInputStream(toDecomp, RecyclingBufferPool.INSTANCE)) {
							decomp.transferTo(wb);
						} catch (IOException e) {
							logger.error(e.getMessage(), e);
						}
					};
					this.future = decompThreads.submit(decompProcess);

					sub.request(1);

				}

				@Override
				public void onNext(ByteBuffer t) {
					transferred += t.remaining();
					try {
						byte[] compressed = new byte[t.remaining()];
						t.get(compressed);
						fromS3.write(compressed);
						sub.request(1);
					} catch (IOException e) {
						cf.completeExceptionally(e);
					}
				}

				@Override
				public void onError(Throwable t) {
					logger.error("onError {}", t.getMessage(), t);
					cf.completeExceptionally(t);
				}

				@Override
				public void onComplete() {
					try {
						fromS3.close();
					} catch (IOException e) {
						// ok, close the pipe
					}
					try {
						future.get(1, TimeUnit.MINUTES);
						cf.complete(null);
					} catch (Exception e) {
						cf.completeExceptionally(e);
					}
				}
			});

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			cf.completeExceptionally(e);
		}

	}

	public long transferred() {
		return transferred;
	}

	@Override
	public void exceptionOccurred(Throwable error) {
		try {
			Files.deleteIfExists(path);
		} catch (IOException e) {
			// don't care
		}
		cf.completeExceptionally(error);
	}

}