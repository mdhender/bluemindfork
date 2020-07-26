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
package net.bluemind.sds.proxy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Promise;
import io.vertx.core.Verticle;
import io.vertx.core.http.HttpServer;
import io.vertx.core.net.SocketAddress;
import net.bluemind.lib.vertx.IVerticleFactory;

public class SdsProxyUdsVerticle extends SdsProxyBaseVerticle {

	private static final Logger logger = LoggerFactory.getLogger(SdsProxyUdsVerticle.class);
	private static String socketPath = socketPath("/var/run/cyrus/socket/bm-sds");

	public static String socketPath(String p) {
		try {
			socketPath = p;
			Path path = Paths.get(p);
			path.toFile().getParentFile().mkdirs();
			Files.deleteIfExists(path);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
		return p;
	}

	public static class SdsProxyUdsFactory implements IVerticleFactory {

		@Override
		public boolean isWorker() {
			return false;
		}

		@Override
		public Verticle newInstance() {
			return new SdsProxyUdsVerticle();
		}

	}

	protected void doListen(Promise<Void> startedResult, HttpServer srv) {
		srv.listen(SocketAddress.domainSocketAddress(socketPath), result -> {
			if (result.succeeded()) {
				logger.info("listening on UDS {}", socketPath);
				startedResult.complete(null);
			} else {
				startedResult.fail(result.cause());
			}
		});
	}

}
