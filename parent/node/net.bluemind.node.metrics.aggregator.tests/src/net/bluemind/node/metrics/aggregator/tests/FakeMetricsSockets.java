/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.node.metrics.aggregator.tests;

import java.nio.file.Paths;
import java.util.Set;

import com.google.common.collect.Sets;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Verticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.SocketAddress;
import net.bluemind.lib.vertx.IVerticleFactory;
import net.bluemind.node.metrics.aggregator.SystemProps;

public class FakeMetricsSockets extends AbstractVerticle {

	public static class Factory implements IVerticleFactory {

		@Override
		public boolean isWorker() {
			return false;
		}

		@Override
		public Verticle newInstance() {
			return new FakeMetricsSockets();
		}

	}

	private static Set<String> services() {
		Set<String> services = Sets.newHashSet("bm-core", "ysnp");
		String dir = System.getProperty(SystemProps.SOCKET_DIR_PROP);
		System.err.println("FAKE DIR: " + dir);
		for (String s : services) {
			String sock = dir + "/metrics-" + s + ".sock";
			Paths.get(sock).toFile().delete();
		}
		return services;
	}

	private static final Set<String> services = services();

	public void start() {
		String dir = System.getProperty(SystemProps.SOCKET_DIR_PROP);

		for (String s : services) {
			String sock = dir + "/metrics-" + s + ".sock";
			SocketAddress addr = SocketAddress.domainSocketAddress(sock);
			HttpServer srv = vertx.createHttpServer(new HttpServerOptions().setTcpNoDelay(true));

			// bm-tika.hprof,meterType=Gauge value=0
			// bm-tika.jvm.nonHeapMemory.used,meterType=Gauge value=48054344

			srv.requestHandler(req -> {
				Buffer resp = Buffer.buffer();
				resp.appendString(s + ".hprof,meterType=Gauge value=0\n");
				resp.appendString(s + ".jvm.nonHeapMemory.used,meterType=Gauge value=48054344\n");
				req.response().end(resp);
				// System.err.println("resp sent for " + s);
			});

			srv.listen(addr, result -> {
				if (result.failed()) {
					result.cause().printStackTrace();
				} else {
					System.err.println("Started on " + addr);
				}
			});
		}

		String blockSock = dir + "/metrics-blocking.sock";
		SocketAddress addr = SocketAddress.domainSocketAddress(blockSock);
		HttpServer srv = vertx.createHttpServer(new HttpServerOptions().setTcpNoDelay(true));

		srv.requestHandler(req -> {
			// just block
		});

		srv.listen(addr, result -> {
			if (result.failed()) {
				result.cause().printStackTrace();
			} else {
				System.err.println("Blocking socket on " + addr);
			}
		});
	}

}
