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
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import com.google.common.io.CountingInputStream;
import com.netflix.spectator.api.Clock;
import com.netflix.spectator.api.Counter;
import com.netflix.spectator.api.DistributionSummary;
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
import net.bluemind.sds.store.scalityring.zstd.ZstdStreams;

public class ScalityRingStore implements ISdsBackingStore {
	private static final Logger logger = LoggerFactory.getLogger(ScalityRingStore.class);
	// This setting must be very high in order to allow getting many many objects in
	// parallel in case of an imap fetch rush from multiple clients
	public static final int PARALLELISM = 32768;

	private final AsyncHttpClient client;
	private final String endpoint;

	private final IdFactory idFactory;
	private final Timer getLatencyTimer;
	private final Timer mgetLatencyTimer;
	private final Timer existLatencyTimer;
	private final Timer putLatencyTimer;
	private final Timer deleteLatencyTimer;
	private final Clock clock;
	private final Counter getSizeCounter;
	private final Counter existRequestCounter;
	private final Counter existFailureRequestCounter;
	private final Counter getRequestCounter;
	private final Counter getFailureRequestCounter;
	private final Counter putRequestCounter;
	private final Counter putFailureRequestCounter;
	private final Counter mgetRequestCounter;
	private final Counter deleteRequestCounter;
	private final Counter deleteFailureRequestCounter;
	private final Counter putSizeCounter;
	private DistributionSummary compressionRatio;

	public ScalityRingStore(ScalityConfiguration configuration, AsyncHttpClient client, Registry registry,
			IdFactory idfactory) {
		this.idFactory = idfactory;
		this.client = client;
		endpoint = configuration.endpoint;

		clock = registry.clock();
		getSizeCounter = registry.counter(idFactory.name("transfer").withTag("direction", "download"));
		getRequestCounter = registry
				.counter(idFactory.name("request").withTag("method", "get").withTag("status", "success"));
		getFailureRequestCounter = registry
				.counter(idFactory.name("request").withTag("method", "get").withTag("status", "error"));
		putRequestCounter = registry
				.counter(idFactory.name("request").withTag("method", "put").withTag("status", "success"));
		putFailureRequestCounter = registry
				.counter(idFactory.name("request").withTag("method", "put").withTag("status", "error"));
		existRequestCounter = registry
				.counter(idFactory.name("request").withTag("method", "exist").withTag("status", "success"));
		existFailureRequestCounter = registry
				.counter(idFactory.name("request").withTag("method", "exist").withTag("status", "error"));
		mgetRequestCounter = registry
				.counter(idFactory.name("request").withTag("method", "mget").withTag("status", "success"));
		deleteRequestCounter = registry
				.counter(idFactory.name("request").withTag("method", "delete").withTag("status", "success"));
		deleteFailureRequestCounter = registry
				.counter(idFactory.name("request").withTag("method", "delete").withTag("status", "error"));
		putSizeCounter = registry.counter(idFactory.name("transfer").withTag("direction", "upload"));
		existLatencyTimer = registry.timer(idFactory.name("latency").withTag("method", "exist"));
		mgetLatencyTimer = registry.timer(idFactory.name("latency").withTag("method", "mget"));
		getLatencyTimer = registry.timer(idFactory.name("latency").withTag("method", "get"));
		putLatencyTimer = registry.timer(idFactory.name("latency").withTag("method", "put"));
		compressionRatio = registry.distributionSummary(idFactory.name("compressionRatio"));
		deleteLatencyTimer = registry.timer(idFactory.name("latency").withTag("method", "delete"));
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
					Counter requestCounter = known ? existRequestCounter : existFailureRequestCounter;
					requestCounter.increment();
					return ExistResponse.from(known);
				}).exceptionally(t -> {
					logger.error("exists request failed", t);
					existFailureRequestCounter.increment();
					return ExistResponse.from(false);
				});
	}

	@Override
	public CompletableFuture<SdsResponse> upload(PutRequest req) {
		final long start = clock.monotonicTime();

		File uploadFile = new File(req.filename);
		long uploadLength = uploadFile.length();

		return exists(ExistRequest.of(req.guid)) //
				.thenApply(er -> er.exists).thenCompose(existresponse -> {
					SdsResponse sr = new SdsResponse();
					sr.withTags(ImmutableMap.of("guid", req.guid, "skip", "true"));
					if (Boolean.TRUE.equals(existresponse)) {
						return CompletableFuture.completedFuture(sr);
					}
					CountingInputStream stream = ZstdStreams.compress(uploadFile);
					return client.preparePut(endpoint + "/" + req.guid) //
							.setBody(stream) //
							.execute() //
							.toCompletableFuture() //
							.thenApply(httpresp -> {
								boolean success = httpresp.getStatusCode() == 200;
								putLatencyTimer.record(clock.monotonicTime() - start, TimeUnit.NANOSECONDS);
								putSizeCounter.increment(uploadLength);
								long compressionPercent = stream.getCount() * 100 / uploadLength;
								compressionRatio.record(Math.max(0, 100 - compressionPercent));
								Counter requestCounter = success ? putRequestCounter : putFailureRequestCounter;
								requestCounter.increment();
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
					putFailureRequestCounter.increment();
					return sr;
				});
	}

	@Override
	public CompletableFuture<SdsResponse> download(GetRequest req) {
		final long start = clock.monotonicTime();
		CompletableFuture<SdsResponse> response = new CompletableFuture<>();
		AtomicLong downloaded = new AtomicLong(0);

		Path downloadPath = Paths.get(req.filename);
		OutputStream outChannel = ZstdStreams.decompress(downloadPath);

		client.prepareGet(endpoint + "/" + req.guid) //
				.execute(new AsyncCompletionHandler<Void>() {
					@Override
					public State onBodyPartReceived(HttpResponseBodyPart bodyPart) throws IOException {
						byte[] data = bodyPart.getBodyPartBytes();
						long len = data.length;
						if (len > 0) {
							downloaded.addAndGet(len);
							outChannel.write(data);
						}
						return State.CONTINUE;
					}

					@Override
					public void onThrowable(Throwable t) {
						try {
							outChannel.close();
						} catch (IOException e) {
						}
						logger.error("download request failed", t);
						getFailureRequestCounter.increment();
						response.completeExceptionally(t);
					}

					@Override
					public Void onCompleted(Response httpresp) throws Exception {
						boolean success = httpresp.getStatusCode() == 200;
						outChannel.close();
						if (!success) {
							getFailureRequestCounter.increment();
							response.completeExceptionally(new RuntimeException("failed to download " + req.guid + ": "
									+ httpresp.getStatusCode() + " " + httpresp.getStatusText()));
							// Don't store the '404 error' page on the local filesystem
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
					deleteLatencyTimer.record(clock.monotonicTime() - start, TimeUnit.NANOSECONDS);
					Counter requestCounter = success ? deleteRequestCounter : deleteFailureRequestCounter;
					requestCounter.increment();
					return SdsResponse.UNTAGGED_OK;
				}).exceptionally(t -> {
					logger.error("delete request failed", t);
					SdsResponse sr = new SdsResponse();
					sr.withTags(ImmutableMap.of("guid", req.guid));
					sr.error = new SdsError(t.getMessage());
					deleteFailureRequestCounter.increment();
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

	@Override
	public void close() {
		if (client != null && !client.isClosed()) {
			try {
				client.close();
			} catch (IOException e) {
				logger.error("Unable to close scality ring store", e);
			}
		}
	}

}
