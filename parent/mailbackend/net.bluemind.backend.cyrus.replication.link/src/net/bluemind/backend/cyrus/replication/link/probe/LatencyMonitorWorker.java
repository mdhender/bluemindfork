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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.backend.cyrus.replication.link.probe;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

import com.netflix.spectator.api.Gauge;
import com.netflix.spectator.api.Registry;

import net.bluemind.config.Token;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.imap.Annotation;
import net.bluemind.imap.Flag;
import net.bluemind.imap.FlagsList;
import net.bluemind.imap.IMAPException;
import net.bluemind.imap.SearchQuery;
import net.bluemind.imap.StoreClient;
import net.bluemind.lib.vertx.IVerticleFactory;
import net.bluemind.metrics.registry.IdFactory;
import net.bluemind.metrics.registry.MetricsRegistry;
import net.bluemind.network.topology.Topology;
import net.bluemind.server.api.Server;

public class LatencyMonitorWorker extends Verticle {

	private static final Logger logger = LoggerFactory.getLogger(LatencyMonitorWorker.class);

	private final Registry registry;
	private final IdFactory idf;

	public LatencyMonitorWorker() {
		registry = MetricsRegistry.get();
		this.idf = new IdFactory("replication-latency", registry, LatencyMonitorWorker.class);
	}

	@Override
	public void start() {
		vertx.eventBus().registerHandler("replication.latency.probe", (Message<JsonObject> msg) -> {
			Probe probe = Probe.of(msg.body());
			doProbe(probe).whenComplete((lat, ex) -> {
				if (ex == null) {
					logger.info("Replication latency is {}ms.", lat);
					msg.reply();
				} else {
					msg.fail(1, ex.getMessage());
				}
			});
		});
	}

	public static class Probe {
		String datalocation;
		String name;
		String domainUid;

		private Probe(String loc, String n, String dom) {
			this.datalocation = loc;
			this.name = n;
			this.domainUid = dom;
		}

		public JsonObject toJson() {
			return new JsonObject().putString("datalocation", datalocation).putString("name", name)
					.putString("domainUid", domainUid);
		}

		public static Probe of(JsonObject js) {
			return new Probe(js.getString("datalocation"), js.getString("name"), js.getString("domainUid"));
		}

		public static Probe of(SharedMailboxProbe p) {
			return new Probe(p.backend().uid, p.share().value.name, p.domainUid());
		}
	}

	public CompletableFuture<Long> doProbe(Probe probe) {
		CompletableFuture<Long> replFeeback = new CompletableFuture<>();
		ItemValue<Server> backend = Topology.get().datalocation(probe.datalocation);
		Gauge latencyGauge = registry.gauge(
				idf.name("probe", "mailbox", probe.name, "unit", "ms", "backendAddress", backend.value.address()));

		FlagsList fl = new FlagsList();
		fl.add(Flag.SEEN);

		try (StoreClient sc = new StoreClient(backend.value.address(), 1143, "admin0", Token.admin0())) {
			if (!sc.login()) {
				throw new IMAPException("Login failed for admin0");
			}
			String mboxName = probe.name + "@" + probe.domainUid;
			boolean selected = sc.select(mboxName);
			if (!selected) {
				throw new IMAPException("SELECT of " + mboxName + " failed.");
			}
			Annotation annot = sc.getAnnotation(mboxName, "/vendor/cmu/cyrus-imapd/uniqueid")
					.get("/vendor/cmu/cyrus-imapd/uniqueid");
			if (annot == null) {
				throw new IMAPException("/vendor/cmu/cyrus-imapd/uniqueid annotation is missing");
			}

			logger.debug("SHARE {} selected => {}, uniqueid => {}", mboxName, selected, annot.valueShared);
			Collection<Integer> content = sc.uidSearch(new SearchQuery());
			if (content.isEmpty()) {
				String eml = "From: tick@bluemind.net\r\nSubject: PROBE\r\n\r\nprobed.\r\n\r\n";
				int addedUid = sc.append(mboxName, new ByteArrayInputStream(eml.getBytes(StandardCharsets.US_ASCII)),
						fl);
				logger.info("SHARE {} added {}", mboxName, addedUid);
				if (addedUid > 0) {
					content = Arrays.asList(addedUid);
				}
			}
			if (content.isEmpty()) {
				throw new IMAPException("Failed to add a message to " + mboxName);
			}
			CompletableFuture<Void> applyMailboxPromise = ReplicationFeebackObserver.addWatcher(vertx,
					annot.valueShared);
			FlagsList curFlags = sc.uidFetchFlags(content).iterator().next();
			boolean set = !curFlags.contains(Flag.SEEN);
			long now = System.currentTimeMillis();
			boolean tagged = sc.uidStore(content, fl, set);
			if (!tagged) {
				replFeeback.completeExceptionally(new IMAPException("Failed to update \\Seen flag (set: " + set + ")"));
			} else {
				applyMailboxPromise.whenComplete((v, ex) -> {
					if (ex != null) {
						latencyGauge.set(60000);
						replFeeback.completeExceptionally(ex);
					} else {
						latencyGauge.set((double) (System.currentTimeMillis() - now));
						replFeeback.complete(System.currentTimeMillis() - now);
					}
				});
			}

		} catch (Exception ex) {
			latencyGauge.set(60000);
			replFeeback.completeExceptionally(ex);
		}
		return replFeeback;

	}

	public static class Factory implements IVerticleFactory {

		@Override
		public boolean isWorker() {
			return true;
		}

		@Override
		public Verticle newInstance() {
			return new LatencyMonitorWorker();
		}

	}

}
