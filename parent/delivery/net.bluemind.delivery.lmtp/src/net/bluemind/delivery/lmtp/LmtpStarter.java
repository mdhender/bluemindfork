/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.delivery.lmtp;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subethamail.smtp.helper.SimpleMessageListenerAdapter;
import org.subethamail.smtp.server.SMTPServer;

import io.netty.util.concurrent.DefaultThreadFactory;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Verticle;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.lib.vertx.IUniqueVerticleFactory;
import net.bluemind.lib.vertx.IVerticleFactory;
import net.bluemind.network.topology.IServiceTopology;
import net.bluemind.network.topology.Topology;

public class LmtpStarter extends AbstractVerticle {

	private static final Logger logger = LoggerFactory.getLogger(LmtpStarter.class);

	@Override
	public void start(Promise<Void> startPromise) {
		SMTPServer dontCare = Topology.getIfAvailable().map(tp -> startImpl(tp, startPromise)).orElseGet(() -> {
			logger.info("Topology is not yet available, delay {} startup a bit.", this);
			vertx.setTimer(2000, tid -> start(startPromise));
			return null;
		});
		logger.debug("smtp is {}", dontCare);
	}

	private SMTPServer startImpl(IServiceTopology t, Promise<Void> startPromise) {

		String url = "http://" + t.any("bm/core").value.address() + ":8090";
		ApiProv prov = k -> ClientSideServiceProvider.getProvider(url, k);

		LmtpMessageHandler msgHandler = new LmtpMessageHandler(prov);
		ExecutorService threadPool = Executors.newCachedThreadPool(new DefaultThreadFactory("lmtp"));
		SMTPServer srv = new SMTPServer(new SimpleMessageListenerAdapter(msgHandler), null, threadPool);
		srv.getCommandHandler().addCommand(new LhloCommand());
		srv.setSoftwareName("bm-lmtpd");
		srv.setPort(2400);
		srv.start();
		startPromise.complete();
		return srv;
	}

	public static class Reg implements IVerticleFactory, IUniqueVerticleFactory {

		@Override
		public boolean isWorker() {
			return false;
		}

		@Override
		public Verticle newInstance() {
			return new LmtpStarter();
		}

	}

}
