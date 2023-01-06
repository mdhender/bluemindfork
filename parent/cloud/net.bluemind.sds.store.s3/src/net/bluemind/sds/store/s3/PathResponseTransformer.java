/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.sds.store.s3;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.Unpooled;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import software.amazon.awssdk.core.async.SdkPublisher;

public class PathResponseTransformer<T> implements IResponseTransformer<T> {
	private static final Logger logger = LoggerFactory.getLogger(PathResponseTransformer.class);

	private static final OpenOptions OPEN_OPTS = new OpenOptions().setCreate(true).setWrite(true)
			.setTruncateExisting(true);

	private final String path;
	private CompletableFuture<T> cf;
	private T response;
	private long transferred;
	private final Vertx vertx;

	public PathResponseTransformer(Vertx vertx, String path) {
		this.vertx = vertx;
		this.path = path;
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

	@Override
	public void onStream(SdkPublisher<ByteBuffer> publisher) {
		vertx.fileSystem().open(path, OPEN_OPTS, res -> {
			if (res.succeeded()) {
				AsyncFile asyncFile = res.result();
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
						Buffer vxBuf = Buffer.buffer(Unpooled.wrappedBuffer(t));
						asyncFile.write(vxBuf, done -> sub.request(1));
					}

					@Override
					public void onError(Throwable t) {
						asyncFile.close();
						exceptionOccurred(t);
					}

					@Override
					public void onComplete() {
						asyncFile.flush(flushed -> asyncFile.close(c -> cf.complete(null)));
					}
				});
			} else {
				exceptionOccurred(res.cause());
			}
		});
	}

	@Override
	public void exceptionOccurred(Throwable error) {
		try {
			Files.deleteIfExists(Paths.get(path));
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
		cf.completeExceptionally(error);
	}

	@Override
	public long transferred() {
		return transferred;
	}

}
