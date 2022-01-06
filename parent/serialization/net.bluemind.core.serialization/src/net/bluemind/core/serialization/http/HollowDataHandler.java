/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
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
package net.bluemind.core.serialization.http;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;
import com.netflix.hollow.api.consumer.HollowConsumer;

import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import net.bluemind.core.rest.http.vertx.NeedVertxExecutor;
import net.bluemind.core.serialization.Activator;
import net.bluemind.core.serialization.DataSerializer;

public class HollowDataHandler implements Handler<HttpServerRequest>, NeedVertxExecutor {

	private Vertx vertx;
	private static final Logger logger = LoggerFactory.getLogger(HollowDataHandler.class);

	@Override
	public void handle(HttpServerRequest request) {
		logger.info("handle {} {}", request.path(), request.params());
		request.endHandler(v -> {
			HttpServerResponse resp = request.response();
			resp.setChunked(true);
			Target target = null;
			try {
				target = Target.fromPath(request.path());
				logger.info("target: {}", target);
			} catch (Exception e) {
				logger.warn("Cannot detect domain and version out of {}", request.path());
				resp.setStatusCode(500).end();
				return;
			}
			logger.debug("Handling request to target {}:{}:{}:{}", target.type.name(), target.set, target.subset,
					target.version);
			if (target.type == BlobType.VERSION) {
				retrieveVersion(target, resp);
			} else {
				retrieveData(target, resp);
			}
		});
	}

	private void retrieveVersion(Target target, HttpServerResponse resp) {
		vertx.executeBlocking(prom -> prom.complete(getVersion(target)), false, ar -> {
			if (ar.failed()) {
				logger.error("retrieveVersion failed", ar.cause());
				resp.setStatusCode(500).setStatusMessage("retrieveVersion " + ar.cause().getMessage());
			} else {
				resp.putHeader("Content-Type", "text/plain");
				resp.write(ar.result() + "");
			}
			resp.end();
		});
	}

	private long getVersion(Target target) {
		return getSerializerBySet(target.set, target.subset).getLastVersion();
	}

	private void retrieveData(Target target, HttpServerResponse resp) {
		vertx.executeBlocking((Promise<BlobData> prom) -> prom.complete(getDataBlob(target)), false, ar -> {
			if (ar.failed()) {
				error(resp, ar.cause(), target);
			} else {
				BlobData blob = ar.result();
				resp.putHeader("Content-Type", "application/octet-stream");
				resp.putHeader("X-BM-DATASET_VERSION", "" + blob.toVersion);
				resp.sendFile(blob.file.getAbsolutePath(), result -> {
					if (!resp.ended()) {
						resp.end();
					}
					blob.file.delete();
				});
			}
		});
	}

	private void error(HttpServerResponse resp, Throwable ex, Target target) {
		resp.setStatusCode(500);
		if (ex != null) {
			logger.error(ex.getMessage(), ex);
			resp.write(ex.getMessage());
		} else {
			resp.write("Cannot retrieve blob " + target.type.name() + ":" + target.set + ":" + target.subset + ":"
					+ target.version);
		}
		resp.end();
	}

	private BlobData getDataBlob(Target target) {
		DataSerializer ds = getSerializerBySet(target.set, target.subset);
		HollowConsumer.Blob blob = getBlobRetriever(ds, target).apply(target.version);
		try {
			File tempFile = File.createTempFile("target.set", "" + System.currentTimeMillis());
			Files.copy(blob.getInputStream(), tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			return new BlobData(tempFile, blob.getToVersion());
		} catch (Exception e) {
			logger.warn("Cannot retrieve blob {}:{}:{}:{}", target.type.name(), target.set, target.subset,
					target.version, e);
			return null;
		}
	}

	private static DataSerializer getSerializerBySet(String set, String subset) {
		return Activator.serializers.stream().filter(s -> s.supportedSet().equals(set)).findFirst()
				.map(o -> o.create(subset))
				.orElseThrow(() -> new IllegalArgumentException("Unknown " + set + "/" + subset));
	}

	public Function<Long, HollowConsumer.Blob> getBlobRetriever(DataSerializer ds, Target target) {
		switch (target.type) {
		case DELTA:
			return version -> ds.getBlobRetriever().retrieveDeltaBlob(version);
		case SNAPSHOT:
		default:
			return version -> ds.getBlobRetriever().retrieveSnapshotBlob(version);
		}
	}

	@Override
	public void setVertxExecutor(Vertx vertx, ExecutorService bmExecutor) {
		this.vertx = vertx;
		logger.info("setVertxExecutor {}", bmExecutor);
	}

	private static class Target {
		public final String set;
		public final String subset;
		public final long version;
		public final BlobType type;
		private static final Pattern pattern = Pattern.compile("/serdata/(.+?)/(.+?)/(.+?)/(delta|snapshot|version)");

		public Target(String set, String subset, long version, BlobType type) {
			this.set = set;
			this.subset = subset;
			this.version = version;
			this.type = type;
		}

		public static Target fromPath(String path) {
			path = path.replaceAll("/$", "").replace("..", "");
			Matcher matcher = pattern.matcher(path);
			if (matcher.matches()) {
				String set = matcher.group(1);
				String subset = matcher.group(2);
				long version = Long.parseLong(matcher.group(3));
				BlobType type = BlobType.valueOf(matcher.group(4).toUpperCase());
				return new Target(set, subset, version, type);
			} else {
				throw new IllegalArgumentException("Cannot detect domain and version out of " + path);
			}
		}

		@Override
		public String toString() {
			return MoreObjects.toStringHelper(Target.class).add("se", set).add("su", subset).add("v", version)
					.add("t", type).toString();
		}

	}

	private static enum BlobType {
		SNAPSHOT, DELTA, VERSION;
	}

	private static class BlobData {
		public final File file;
		public final long toVersion;

		public BlobData(File file, long version) {
			this.file = file;
			this.toVersion = version;
		}
	}

}
