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
package net.bluemind.sds.store.scalityring;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

import org.asynchttpclient.AsyncCompletionHandler;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.HttpResponseBodyPart;
import org.asynchttpclient.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.netflix.spectator.api.Clock;
import com.netflix.spectator.api.Counter;
import com.netflix.spectator.api.Registry;
import com.netflix.spectator.api.Timer;

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

public class ScalityRingStore implements ISdsBackingStore {
	private static final Logger logger = LoggerFactory.getLogger(ScalityRingStore.class);
	// This setting must be very high in order to allow getting many many objects in
	// parallel in case of an imap fetch rush from multiple clients
	public static final int PARALLELISM = 32768;

	private final AsyncHttpClient client;
	private final String endpoint;
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

	public ScalityRingStore(ScalityConfiguration configuration, AsyncHttpClient client, Registry registry,
			IdFactory idfactory) {
		this.registry = registry;
		this.idFactory = idfactory;
		this.client = client;
		endpoint = configuration.endpoint;

		clock = registry.clock();
		getLatencyTimer = registry.timer(idFactory.name("latency").withTag("method", "get"));
		getSizeCounter = registry.counter(idFactory.name("transfer").withTag("direction", "download"));
		getRequestCounter = registry
				.counter(idFactory.name("request").withTag("method", "get").withTag("status", "success"));
		getFailureRequestCounter = registry
				.counter(idFactory.name("request").withTag("method", "get").withTag("status", "error"));
		mgetLatencyTimer = registry.timer(idFactory.name("latency").withTag("method", "mget"));
		mgetRequestCounter = registry
				.counter(idFactory.name("request").withTag("method", "mget").withTag("status", "success"));
		existLatencyTimer = registry.timer(idFactory.name("latency").withTag("method", "exist"));
	}

	@Override
	public CompletableFuture<ExistResponse> exists(ExistRequest req) {
		final long start = clock.monotonicTime();
		return client.prepareHead(endpoint + "/" + req.guid) //
				// Makes no attempt to return object metadata,
				// and the “X-Scal-Usermd” header is not included in the response.
				// This may avoid some disk reads when system metadata is in memory.
				.addHeader("X-Scal-Get-Usermd", "No") //
				.execute() //
				.toCompletableFuture() //
				.thenApply(httpresp -> {
					existLatencyTimer.record(clock.monotonicTime() - start, TimeUnit.NANOSECONDS);
					boolean known = httpresp.getStatusCode() == 200;
					registry.counter(idFactory.name("request").withTag("method", "exist").withTag("status",
							known ? "success" : "error")).increment();
					return ExistResponse.from(known);
				}).exceptionally(t -> {
					logger.error("exists request failed", t);
					registry.counter(idFactory.name("request").withTag("method", "exist").withTag("status", "error"))
							.increment();
					return ExistResponse.from(false);
				});
	}

	@Override
	public CompletableFuture<SdsResponse> upload(PutRequest req) {
		final long start = clock.monotonicTime();

		File uploadFile = new File(req.filename);
		long uploadLength = uploadFile.length();

		return exists(ExistRequest.of(req.guid)) //
				.thenApply(er -> {
					return er.exists;
				}).thenCompose(existresponse -> {
					SdsResponse sr = new SdsResponse();
					sr.withTags(ImmutableMap.of("guid", req.guid, "skip", "true"));
					if (Boolean.TRUE.equals(existresponse)) {
						return CompletableFuture.completedFuture(sr);
					}
					return client.preparePut(endpoint + "/" + req.guid) //
							.setBody(uploadFile) //
							.execute() //
							.toCompletableFuture() //
							.thenApply(httpresp -> {
								boolean success = httpresp.getStatusCode() == 200;
								registry.timer(idFactory.name("latency").withTag("method", "put"))
										.record(clock.monotonicTime() - start, TimeUnit.NANOSECONDS);
								registry.counter(idFactory.name("transfer").withTag("direction", "upload"))
										.increment(uploadLength);
								registry.counter(idFactory.name("request").withTag("method", "put").withTag("status",
										success ? "success" : "error")).increment();
								if (!success) {
									String statusMessage = httpresp.getStatusText();
									sr.error = new SdsError(statusMessage != null ? statusMessage : "no response text");
								}
								return sr;
							});
				}).exceptionally(t -> {
					logger.error("put request failed", t);
					SdsResponse sr = new SdsResponse();
					sr.withTags(ImmutableMap.of("guid", req.guid));
					sr.error = new SdsError(t.getMessage());
					registry.counter(idFactory.name("request").withTag("method", "put").withTag("status", "error"))
							.increment();
					return sr;
				});
	}

	@Override
	public CompletableFuture<SdsResponse> download(GetRequest req) {
		final long start = clock.monotonicTime();
		CompletableFuture<SdsResponse> response = new CompletableFuture<>();
		SeekableByteChannel outChannel;
		AtomicLong downloaded = new AtomicLong(0);

		Path downloadPath = Paths.get(req.filename);
		try {
			outChannel = Files.newByteChannel(downloadPath, StandardOpenOption.CREATE, // NOSONAR
					StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
			response.whenComplete((r, t) -> {
				try {
					outChannel.close();
				} catch (IOException e) {
					logger.error("error closing output channel", e);
				}
			});
		} catch (IOException e) {
			logger.error("output stream open failed");
			response.completeExceptionally(e);
			return response;
		}

		client.prepareGet(endpoint + "/" + req.guid) //
				.execute(new AsyncCompletionHandler<Void>() {
					@Override
					public State onBodyPartReceived(HttpResponseBodyPart bodyPart) throws IOException {
						ByteBuffer data = bodyPart.getBodyByteBuffer();
						long len = data.remaining();
						if (len > 0) {
							downloaded.addAndGet(len);
							outChannel.write(data);
						}
						return State.CONTINUE;
					}

					@Override
					public void onThrowable(Throwable t) {
						logger.error("download request failed", t);
						getFailureRequestCounter.increment();
						response.completeExceptionally(t);
					}

					@Override
					public Void onCompleted(Response httpresp) throws Exception {
						boolean success = httpresp.getStatusCode() == 200;
						if (!success) {
							getFailureRequestCounter.increment();
							response.completeExceptionally(new RuntimeException("failed to download " + req.guid + ": "
									+ httpresp.getStatusCode() + " " + httpresp.getStatusText()));
							// Don't store the '404 error' page on the local filesystem
							outChannel.close();
							Files.deleteIfExists(downloadPath);
							return null;
						}
						getLatencyTimer.record(clock.monotonicTime() - start, TimeUnit.NANOSECONDS);
						getRequestCounter.increment();
						getSizeCounter.increment(downloaded.get());
						response.complete(new SdsResponse().withSize(downloaded.get()));
						return null;
					}
				});
		return response;
	}

	@Override
	public CompletableFuture<SdsResponse> delete(DeleteRequest req) {
		final long start = clock.monotonicTime();
		return client.prepareDelete(endpoint + "/" + req.guid) //
				// Makes no attempt to return object metadata,
				// and the “X-Scal-Usermd” header is not included in the response.
				// This may avoid some disk reads when system metadata is in memory.
				.execute() //
				.toCompletableFuture() //
				.thenApply(httpresp -> {
					boolean success = httpresp.getStatusCode() == 200;
					registry.timer(idFactory.name("latency").withTag("method", "delete"))
							.record(clock.monotonicTime() - start, TimeUnit.NANOSECONDS);
					registry.counter(idFactory.name("request").withTag("method", "delete").withTag("status",
							success ? "success" : "error")).increment();
					return SdsResponse.UNTAGGED_OK;
				}).exceptionally(t -> {
					logger.error("delete request failed", t);
					SdsResponse sr = new SdsResponse();
					sr.withTags(ImmutableMap.of("guid", req.guid));
					sr.error = new SdsError(t.getMessage());
					registry.counter(idFactory.name("request").withTag("method", "delete").withTag("status", "error"))
							.increment();
					return sr;
				});
	}

	@Override
	public CompletableFuture<SdsResponse> downloads(MgetRequest req) {
		final long start = clock.monotonicTime();

		int len = req.transfers.size();
		final LongAdder totalSize = new LongAdder();
		int parallelStreams = Math.min(PARALLELISM / 8, 32);
		CompletableFuture<?>[] roots = new CompletableFuture[parallelStreams];
		for (int i = 0; i < parallelStreams; i++) {
			roots[i] = CompletableFuture.completedFuture(null);
		}
		Iterator<Transfer> it = req.transfers.iterator();
		for (int i = 0; i < len; i++) {
			int slot = i % parallelStreams;
			Transfer t = it.next();

			roots[slot] = roots[slot]
					.thenCompose(v -> download(GetRequest.of(req.mailbox, t.guid, t.filename)).thenApply(sdsresp -> {
						totalSize.add(sdsresp.size());
						return sdsresp;
					}));
		}

		return CompletableFuture.allOf(roots).thenApply(v -> {
			mgetLatencyTimer.record(clock.monotonicTime() - start, TimeUnit.NANOSECONDS);
			getSizeCounter.increment(totalSize.longValue());
			mgetRequestCounter.increment();
			String sizeKb = Long.toString(totalSize.longValue() / 1024);
			return new SdsResponse().withTags(ImmutableMap.of("batch", Integer.toString(len), "sizeKB", sizeKb));
		}).exceptionally(ex -> {
			logger.error(ex.getMessage() + " for " + req, ex);
			SdsResponse error = new SdsResponse();
			error.error = new SdsError(ex.getMessage());
			return error;
		});
	}

}
