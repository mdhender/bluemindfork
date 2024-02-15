/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.lib.vertx.metrics;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.base.Stopwatch;
import com.netflix.spectator.api.Counter;
import com.netflix.spectator.api.Registry;
import com.netflix.spectator.api.Timer;
import com.netflix.spectator.api.patterns.PolledMeter;

import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.metrics.MetricsOptions;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.spi.VertxMetricsFactory;
import io.vertx.core.spi.metrics.EventBusMetrics;
import io.vertx.core.spi.metrics.HttpServerMetrics;
import io.vertx.core.spi.metrics.PoolMetrics;
import io.vertx.core.spi.metrics.TCPMetrics;
import io.vertx.core.spi.metrics.VertxMetrics;
import io.vertx.core.spi.observability.HttpRequest;
import io.vertx.core.spi.observability.HttpResponse;
import net.bluemind.metrics.registry.IdFactory;
import net.bluemind.metrics.registry.MetricsRegistry;

public class SpectatorMetricsOptions extends MetricsOptions implements VertxMetricsFactory, VertxMetrics {

	private final SpectatorEventBusMetrics eventBusMetrics;
	private final Map<String, SpectatorTcpMetrics> netServerMetricsByPort;
	private final Map<String, SpectatorHttpMetrics> httpServerMetricsByPort;
	private final Map<String, SpectatorPoolMetrics> poolByTypeSlashName;

	public SpectatorMetricsOptions() {
		this.eventBusMetrics = new SpectatorEventBusMetrics();
		this.netServerMetricsByPort = new ConcurrentHashMap<>(8);
		this.httpServerMetricsByPort = new ConcurrentHashMap<>(8);
		this.poolByTypeSlashName = new ConcurrentHashMap<>(8);
	}

	@Override
	public VertxMetricsFactory getFactory() {
		return this;
	}

	@Override
	public VertxMetrics metrics(VertxOptions options) {
		return this;
	}

	@Override
	public EventBusMetrics<Void> createEventBusMetrics() {
		return eventBusMetrics;
	}

	@Override
	public PoolMetrics<?> createPoolMetrics(String poolType, String poolName, int maxPoolSize) {
		return poolByTypeSlashName.computeIfAbsent(poolType + "-" + poolName, SpectatorPoolMetrics::new);
	}

	@Override
	public TCPMetrics<Void> createNetServerMetrics(NetServerOptions options, SocketAddress localAddress) {
		return netServerMetricsByPort.computeIfAbsent(port(localAddress), SpectatorTcpMetrics::new);
	}

	private String port(SocketAddress sa) {
		return sa.isDomainSocket() ? sa.path().replace("/", ".") : "%d".formatted(sa.port());
	}

	@Override
	public HttpServerMetrics<Stopwatch, Void, Void> createHttpServerMetrics(HttpServerOptions options,
			SocketAddress localAddress) {
		return httpServerMetricsByPort.computeIfAbsent(port(localAddress), SpectatorHttpMetrics::new);
	}

	public static class SpectatorEventBusMetrics implements EventBusMetrics<Void> {

		private Counter pubSent;
		private Counter privSent;
		private Counter pubRecv;
		private Counter privRecv;
		private static final String PUB_TAG = "publish";

		public SpectatorEventBusMetrics() {
			Registry reg = MetricsRegistry.get();
			IdFactory idf = new IdFactory("eventbus", reg, SpectatorEventBusMetrics.class);
			pubSent = reg.counter(idf.name("messageSent", PUB_TAG, "true"));
			privSent = reg.counter(idf.name("messageSent", PUB_TAG, "false"));
			pubRecv = reg.counter(idf.name("messageReceived", PUB_TAG, "true"));
			privRecv = reg.counter(idf.name("messageReceived", PUB_TAG, "false"));
		}

		@Override
		public void messageSent(String address, boolean publish, boolean local, boolean remote) {
			if (publish) {
				pubSent.increment();
			} else {
				privSent.increment();
			}
		}

		@Override
		public void messageReceived(String address, boolean publish, boolean local, int handlers) {
			if (publish) {
				pubRecv.increment();
			} else {
				privRecv.increment();
			}
		}

	}

	public static class SpectatorPoolMetrics implements PoolMetrics<Stopwatch> {

		private final Timer inQueue;
		private final Timer runSuccess;
		private final Timer runFailed;
		private final Counter rejections;

		private static final String LBL_TAG = "label";

		public SpectatorPoolMetrics(String typeDashName) {
			Registry reg = MetricsRegistry.get();
			IdFactory idf = new IdFactory("pool", reg, SpectatorPoolMetrics.class);
			this.inQueue = reg.timer(idf.name("queue-latency", LBL_TAG, typeDashName));
			this.rejections = reg.counter(idf.name("queue-rejects", LBL_TAG, typeDashName));
			this.runSuccess = reg.timer(idf.name("runtime", LBL_TAG, typeDashName, "status", "success"));
			this.runFailed = reg.timer(idf.name("runtime", LBL_TAG, typeDashName, "status", "failed"));
		}

		@Override
		public Stopwatch submitted() {
			return Stopwatch.createStarted();
		}

		@Override
		public void rejected(Stopwatch t) {
			rejections.increment();
		}

		@Override
		public Stopwatch begin(Stopwatch t) {
			inQueue.record(t.elapsed());
			return t.reset().start();
		}

		@Override
		public void end(Stopwatch t, boolean succeeded) {
			if (succeeded) {
				runSuccess.record(t.elapsed());
			} else {
				runFailed.record(t.elapsed());
			}
		}

	}

	public static class SpectatorHttpMetrics implements HttpServerMetrics<Stopwatch, Void, Void> {
		private final Timer ttfb;

		public SpectatorHttpMetrics(String port) {
			Registry reg = MetricsRegistry.get();
			IdFactory idf = new IdFactory("httpserver-%s".formatted(port), reg, SpectatorEventBusMetrics.class);
			this.ttfb = reg.timer(idf.name("ttfb"));
		}

		@Override
		public Stopwatch requestBegin(Void socketMetric, HttpRequest request) {
			return Stopwatch.createStarted();
		}

		@Override
		public void responseBegin(Stopwatch requestMetric, HttpResponse response) {
			ttfb.record(requestMetric.elapsed());
		}

	}

	public static class SpectatorTcpMetrics implements TCPMetrics<Void> {
		private final AtomicInteger active;
		private final Counter read;
		private final Counter written;

		public SpectatorTcpMetrics(String port) {
			Registry reg = MetricsRegistry.get();
			IdFactory idf = new IdFactory("netserver-%s".formatted(port), reg, SpectatorEventBusMetrics.class);
			active = new AtomicInteger();
			PolledMeter.using(reg).withId(idf.name("connections")).monitorValue(active);
			this.read = reg.counter(idf.name("readBytes"));
			this.written = reg.counter(idf.name("writtenBytes"));
		}

		@Override
		public Void connected(SocketAddress remoteAddress, String remoteName) {
			active.incrementAndGet();
			return null;
		}

		@Override
		public void disconnected(Void socketMetric, SocketAddress remoteAddress) {
			active.decrementAndGet();
		}

		@Override
		public void bytesRead(Void socketMetric, SocketAddress remoteAddress, long numberOfBytes) {
			read.increment(numberOfBytes);
		}

		@Override
		public void bytesWritten(Void socketMetric, SocketAddress remoteAddress, long numberOfBytes) {
			written.increment(numberOfBytes);
		}
	}
}
