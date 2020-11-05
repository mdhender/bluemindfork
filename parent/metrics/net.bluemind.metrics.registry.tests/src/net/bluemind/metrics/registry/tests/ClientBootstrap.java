/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.metrics.registry.tests;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.management.ManagementFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollDomainSocketChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueDomainSocketChannel;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.unix.DomainSocketAddress;
import io.netty.channel.unix.DomainSocketChannel;

public class ClientBootstrap {

	private static final Logger logger = LoggerFactory.getLogger(ClientBootstrap.class);

	public static Bootstrap create(ChannelInitializer<DomainSocketChannel> chanInit) throws FileNotFoundException {
		Bootstrap bootstrap = new Bootstrap();

		Class<? extends Channel> socketClass = null;

		if (Epoll.isAvailable()) {
			// linux stuff
			EpollEventLoopGroup group = new EpollEventLoopGroup(1);
			bootstrap.group(group);
			socketClass = EpollDomainSocketChannel.class;
		} else if (KQueue.isAvailable()) {
			// mac stuff
			KQueueEventLoopGroup group = new KQueueEventLoopGroup(1);
			bootstrap.group(group);
			socketClass = KQueueDomainSocketChannel.class;
		}
		String bluemindProduct = System.getProperty("net.bluemind.property.product",
				ManagementFactory.getRuntimeMXBean().getName());
		String socketName = "/metrics-" + bluemindProduct + ".sock";
		File sock = new File("/var/run/bm-metrics/" + socketName);
		if (!sock.exists()) {
			throw new FileNotFoundException("BMRegistry didn't find file : /var/run/bm-metrics/" + socketName);
		}
		logger.info("Will bind to {}", sock);
		bootstrap.remoteAddress(new DomainSocketAddress(sock));
		bootstrap.channel(socketClass);
		bootstrap.handler(chanInit);
		return bootstrap;
	}

}
