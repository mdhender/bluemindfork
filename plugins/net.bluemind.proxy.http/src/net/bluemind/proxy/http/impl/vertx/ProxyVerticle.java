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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.platform.Verticle;

import net.bluemind.proxy.http.Activator;
import net.bluemind.proxy.http.ILogoutListener;
import net.bluemind.proxy.http.config.ConfigBuilder;
import net.bluemind.proxy.http.config.HPSConfiguration;
import net.bluemind.proxy.http.impl.SessionStore;

public class ProxyVerticle extends Verticle {

	@SuppressWarnings("unused")
	private final Logger logger = LoggerFactory.getLogger(ProxyVerticle.class);

	private CoreState coreState;

	private static HPSConfiguration conf;
	private static SessionStore ss;

	static {
		sharedStuff();
	}

	private static void sharedStuff() {
		conf = ConfigBuilder.build();
		ss = new SessionStore(conf);
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

	public ProxyVerticle() {
	}

	public void start() {
		coreState = new CoreState(vertx);
		coreState.start();
		HttpServer proxy = vertx.createHttpServer();
		proxy.setTCPNoDelay(true);
		proxy.setUsePooledBuffers(true);
		proxy.setTCPKeepAlive(true);
		HpsReqHandler rh = new HpsReqHandler(vertx, conf, ss, coreState);
		proxy.requestHandler(rh);
		proxy.listen(8079);
	}

}
