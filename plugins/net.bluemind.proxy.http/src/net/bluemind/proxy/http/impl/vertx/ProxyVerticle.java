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
package net.bluemind.proxy.http.impl.vertx;

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Suppliers;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import net.bluemind.network.topology.Topology;
import net.bluemind.proxy.http.Activator;
import net.bluemind.proxy.http.ILogoutListener;
import net.bluemind.proxy.http.config.ConfigBuilder;
import net.bluemind.proxy.http.config.HPSConfiguration;
import net.bluemind.proxy.http.impl.SessionStore;

public class ProxyVerticle extends AbstractVerticle {

	@SuppressWarnings("unused")
	private final Logger logger = LoggerFactory.getLogger(ProxyVerticle.class);

	private CoreState coreState;

	private static Supplier<HPSConfiguration> conf;
	private static SessionStore ss;

	static {
		sharedStuff();
	}

	private static void sharedStuff() {
		conf = Suppliers.memoize(ConfigBuilder::build);
		ss = new SessionStore();
		Activator.registerSessionListener(new ILogoutListener() {

			@Override
			public void loggedOut(String sessionId) {
				// FIXME what is that ?!
				ss.purgeSession(sessionId);
			}

			@Override
			public void loggedOutAll() {
				ss.purgeAllSessions();
			}
		});
	}

	@Override
	public void start(Promise<Void> p) {
		if (!Topology.getIfAvailable().isPresent()) {
			logger.warn("Topology missing for {}", this);
		}

		coreState = new CoreState(vertx);
		coreState.start();
		HttpServerOptions opts = new HttpServerOptions();
		opts.setTcpNoDelay(true);
		opts.setUsePooledBuffers(true);
		opts.setTcpKeepAlive(true);
		HttpServer proxy = vertx.createHttpServer(opts);

		HpsReqHandler rh = new HpsReqHandler(vertx, conf.get(), ss, coreState);
		proxy.requestHandler(rh);
		proxy.listen(8079);
		p.complete();
	}

}
