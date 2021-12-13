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

import java.util.List;
import java.util.concurrent.TimeUnit;
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
import net.bluemind.proxy.http.auth.api.IAuthEnforcer;
import net.bluemind.proxy.http.auth.impl.Enforcers;
import net.bluemind.proxy.http.config.ConfigBuilder;
import net.bluemind.proxy.http.config.HPSConfiguration;
import net.bluemind.proxy.http.impl.SessionStore;

public class ProxyVerticle extends AbstractVerticle {

	@SuppressWarnings("unused")
	private final Logger logger = LoggerFactory.getLogger(ProxyVerticle.class);

	private CoreState coreState = new CoreState(vertx);

	private static Supplier<HPSConfiguration> conf;
	static SessionStore ss = new SessionStore();

	static {
		sharedStuff();
	}

	private static void sharedStuff() {
		conf = Suppliers.memoize(ConfigBuilder::build);

		Activator.registerSessionListener(new ILogoutListener() {
			@Override
			public void loggedOut(String sessionId) {
				// FIXME what is that ?!
				ss.purgeSession(sessionId);
			}

			@Override
			public void checkAll() {
				ss.checkAll();
			}
		});
	}

	@Override
	public void start(Promise<Void> p) {
		if (!Topology.getIfAvailable().isPresent()) {
			logger.warn("Topology missing for {}", this);
		}

		// Every day, remove sessions files from disk that are not in cache
		vertx.setPeriodic(TimeUnit.DAYS.toMillis(1), i -> ss.cleanUp());

		HttpServerOptions opts = new HttpServerOptions();
		opts.setTcpNoDelay(true);
		opts.setTcpKeepAlive(true);
		HttpServer proxy = vertx.createHttpServer(opts);

		List<IAuthEnforcer> authEnforcers = Enforcers.enforcers(vertx);
		ss.addAuthEnforcers(authEnforcers);

		HpsReqHandler rh = new HpsReqHandler(vertx, conf.get(), authEnforcers, ss, coreState);
		proxy.requestHandler(rh);
		proxy.listen(8079);
		p.complete();
	}
}
