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

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.StandardSocketOptions;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subethamail.smtp.MessageHandlerFactory;
import org.subethamail.smtp.helper.SimpleMessageListenerAdapter;
import org.subethamail.smtp.server.SMTPServer;

import io.netty.util.concurrent.DefaultThreadFactory;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Verticle;
import net.bluemind.imap.serviceprovider.SPResolver;
import net.bluemind.lib.vertx.IUniqueVerticleFactory;
import net.bluemind.lib.vertx.IVerticleFactory;
import net.bluemind.network.topology.IServiceTopology;
import net.bluemind.network.topology.Topology;

public class LmtpStarter extends AbstractVerticle {

	private static final Logger logger = LoggerFactory.getLogger(LmtpStarter.class);

	public class ReuseAddrSMTPServer extends SMTPServer {
		public ReuseAddrSMTPServer(MessageHandlerFactory handlerFactory, ExecutorService threadPool) {
			super(handlerFactory, null, threadPool);
		}

		@Override
		protected ServerSocket createServerSocket() throws IOException {
			InetSocketAddress isa;
			InetAddress bindAddress = getBindAddress();
			int port = getPort();
			if (bindAddress == null) {
				isa = new InetSocketAddress(port);
			} else {
				isa = new InetSocketAddress(bindAddress, port);
			}

			ServerSocket serverSocket = new ServerSocket(); // NOSONAR: no autoclosable here
			serverSocket.setReuseAddress(true);
			serverSocket.bind(isa, this.getBacklog());
			if (port == 0) {
				this.setPort(serverSocket.getLocalPort());
			}
			return serverSocket;
		}
	}

	@Override
	public void start() {
		SMTPServer dontCare = Topology.getIfAvailable().map(this::startImpl).orElseGet(() -> {
			logger.info("Topology is not yet available, delay {} startup a bit.", this);
			vertx.setTimer(2000, tid -> start());
			return null;
		});
		logger.debug("smtp is {}", dontCare);
	}

	private SMTPServer startImpl(IServiceTopology t) {

		ApiProv prov = k -> SPResolver.get().resolve(k);

		LmtpMessageHandler msgHandler = new LmtpMessageHandler(prov);
		ExecutorService threadPool = Executors.newCachedThreadPool(new DefaultThreadFactory("lmtp"));
		SMTPServer srv = new ReuseAddrSMTPServer(new SimpleMessageListenerAdapter(msgHandler), threadPool);
		srv.getCommandHandler().addCommand(new LhloCommand());
		srv.setSoftwareName("bm-lmtpd");
		srv.setPort(2400);
		srv.start();
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
