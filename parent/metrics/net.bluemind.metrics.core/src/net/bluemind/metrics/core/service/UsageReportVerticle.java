/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2024
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
package net.bluemind.metrics.core.service;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Verticle;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import net.bluemind.lib.vertx.IUniqueVerticleFactory;
import net.bluemind.lib.vertx.IVerticleFactory;
import net.bluemind.network.topology.Topology;
import net.bluemind.server.api.TagDescriptor;

public class UsageReportVerticle extends AbstractVerticle {

	private static final Logger logger = LoggerFactory.getLogger(UsageReportVerticle.class);
	private final HttpClient client;

	public UsageReportVerticle() {
		this.client = HttpClient.newBuilder().followRedirects(Redirect.NEVER).build();
	}

	@Override
	public void start() throws Exception {
		vertx.setTimer(TimeUnit.MINUTES.toMillis(2), this::usageFromMetrics);
		vertx.setPeriodic(TimeUnit.HOURS.toMillis(1), this::usageFromMetrics);
	}

	private void usageFromMetrics(long unused) {
		try {
			int mapi = max(query(
					"SELECT max(\"value\") AS mapi FROM \"telegraf\".\"autogen\".\"bm-mapi.activeSessions.distinctUsers\" WHERE time > now() - 7d"));
			int imap = max(query(
					"SELECT max(\"value\") AS \"imap\" FROM \"telegraf\".\"autogen\".\"bm-core.netserver-1143.connections\" WHERE time > now() - 7d"));
			int eas = max(query(
					"SELECT max(\"value\") AS \"eas\" FROM \"telegraf\".\"autogen\".\"bm-eas.activeSessions.devices\" WHERE time > now() - 7d"));
			int delivered = max(query(
					"SELECT non_negative_difference(last(\"count\")) AS \"mean_count\" FROM \"telegraf\".\"autogen\".\"bm-core.bm-lmtpd.deliveries\" WHERE time > now() - 7d GROUP BY time(7d)"));

			JsonObject usage = new JsonObject();
			usage//
					.put("mapi", mapi).put("mobile", eas).put("imap", imap).put("delivered", delivered)//
					.put("purpose", "bm-pimp will prefer those values over /etc/bm/usage-prediction.json")//
					.put("comment", "Values are computed using metrics from the past 7 days");
			Files.write(usage.encodePrettily().getBytes(), Paths.get("/etc/bm/usage-from-metrics.json").toFile());
			File prediction = Paths.get("/etc/bm/usage-prediction.json").toFile();
			if (!prediction.exists()) {
				JsonObject pred = new JsonObject();
				pred//
						.put("mapi", 0).put("mobile", 0).put("imap", 0).put("delivered", 0)//
						.put("purpose",
								"bm-pimp will consider those estimates to size jvm memories. usage-from-metrics.json will be used if bigger")//
						.put("comment", "Values are edited by you");
				Files.write(pred.encodePrettily().getBytes(), prediction);
			}

		} catch (Exception e) {
			logger.warn("Usage reporting failed", e);
		}
	}

	private int max(JsonObject influxResult) {
		try {
			JsonArray values = influxResult.getJsonArray("results").getJsonObject(0).getJsonArray("series")
					.getJsonObject(0).getJsonArray("values").getJsonArray(0);
			if (values == null || values.size() == 0) {
				return 0;
			} else {
				return values.getInteger(1);
			}
		} catch (Exception e) {
			return 0;
		}
	}

	public JsonObject query(String select) throws URISyntaxException {
		String ip = Topology.getIfAvailable().flatMap(t -> t.anyIfPresent(TagDescriptor.bm_metrics_influx.name()))
				.map(iv -> iv.value.address()).orElse("127.0.0.1");

		URI url = new URI("http://%s:8086/query".formatted(ip));

		HttpRequest req = HttpRequest.newBuilder()//
				.version(Version.HTTP_1_1)//
				.uri(url)//
				.POST(BodyPublishers.ofString("q=" + select))//
				.header("Content-Type", "application/x-www-form-urlencoded")//
				.build();
		return client.sendAsync(req, BodyHandlers.ofString()).orTimeout(5, TimeUnit.SECONDS)
				.thenApply(HttpResponse::body).thenApply(JsonObject::new).join();
	}

	public static class Reg implements IVerticleFactory, IUniqueVerticleFactory {

		@Override
		public boolean isWorker() {
			return true;
		}

		@Override
		public Verticle newInstance() {
			return new UsageReportVerticle();
		}

	}

}
