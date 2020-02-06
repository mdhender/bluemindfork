/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.lmtp.testhelper.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;

public class MockServerVerticle extends AbstractVerticle {
	private static final Logger logger = LoggerFactory.getLogger(MockServerVerticle.class);
	private NetServer srv;

	public void start(Future<Void> done) {
		this.srv = vertx.createNetServer(new NetServerOptions().setTcpNoDelay(true));
		srv.connectHandler(sock -> {
			LmtpServerSession session = new LmtpServerSession(vertx, sock);
			session.start();
		});
		srv.listen(2424, asyncRes -> {
			if (asyncRes.succeeded()) {
				logger.info("Listening on 2424.");
				done.complete(null);
			} else {
				logger.error(asyncRes.cause().getMessage(), asyncRes);
				done.fail(asyncRes.cause());
			}
		});
	}

	@Override
	public void stop() throws Exception {
		srv.close();
		super.stop();
	}

}
