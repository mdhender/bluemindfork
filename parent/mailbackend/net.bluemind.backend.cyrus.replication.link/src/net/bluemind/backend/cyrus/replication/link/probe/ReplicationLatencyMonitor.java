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
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Vertx;

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
import net.bluemind.metrics.registry.IdFactory;
import net.bluemind.metrics.registry.MetricsRegistry;
import net.bluemind.network.topology.Topology;
import net.bluemind.server.api.Server;

public class ReplicationLatencyMonitor {

	private static final Logger logger = LoggerFactory.getLogger(ReplicationLatencyMonitor.class);

	private final Vertx vertx;
	private final SharedMailboxProbe probe;
	private final Registry registry;

	private final Gauge latencyGauge;

	public ReplicationLatencyMonitor(Vertx vx, SharedMailboxProbe probe) {
		this.vertx = vx;
		this.probe = probe;
		registry = MetricsRegistry.get();
		IdFactory idf = new IdFactory("replication-latency", registry, ReplicationLatencyMonitor.class);
		this.latencyGauge = registry.gauge(idf.name("probe", "mailbox", probe.share().value.name, "unit", "ms",
				"backendAddress", probe.backend().value.address()));
		this.latencyGauge.set(30000);
	}

	public void start() {
		vertx.setTimer(10000, tid -> {
			try {
				CompletableFuture<Long> replicationFeedback = doProbe();
				replicationFeedback.whenComplete((latency, ex) -> {
					if (ex != null) {
						latencyGauge.set(60000);
						if (ex instanceof TimeoutException) {
							logger.warn("Replication is lagging (>60sec) or broken: {}", ex.getMessage());
							vertx.setTimer(10000, retid -> start());
						} else {
							logger.error("Stopping replication probe", ex);
						}
					} else {
						logger.info("Replication latency is {}ms.", latency);
						latencyGauge.set(latency);
						vertx.setTimer(10000, retid -> start());
					}
				});
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		});
	}

	public CompletableFuture<Long> doProbe() {
		CompletableFuture<Long> replFeeback = new CompletableFuture<>();
		ItemValue<Server> backend = Topology.get().datalocation(probe.share().value.dataLocation);

		FlagsList fl = new FlagsList();
		fl.add(Flag.SEEN);

		try (StoreClient sc = new StoreClient(backend.value.address(), 1143, "admin0", Token.admin0())) {
			if (!sc.login()) {
				throw new IMAPException("Login failed for admin0");
			}
			String mboxName = probe.share().value.name + "@" + probe.domainUid();
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
			ReplicationFeebackObserver.toWatch.add(annot.valueShared);
			CompletableFuture<Void> applyMailboxPromise = ReplicationFeebackObserver.addWatcher(vertx);
			FlagsList curFlags = sc.uidFetchFlags(content).iterator().next();
			boolean set = !curFlags.contains(Flag.SEEN);
			long now = System.currentTimeMillis();
			boolean tagged = sc.uidStore(content, fl, set);
			if (!tagged) {
				replFeeback.completeExceptionally(new IMAPException("Failed to update \\Seen flag (set: " + set + ")"));
			} else {
				applyMailboxPromise.whenComplete((v, ex) -> {
					if (ex != null) {
						replFeeback.completeExceptionally(ex);
					} else {
						replFeeback.complete(System.currentTimeMillis() - now);
					}
				});
			}

		} catch (Exception ex) {
			replFeeback.completeExceptionally(ex);
		}
		return replFeeback;

	}

}
