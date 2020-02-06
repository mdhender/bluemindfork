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
package net.bluemind.xivo.bridge.impl;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import net.bluemind.config.InstallationId;
import net.bluemind.config.Token;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.locator.client.LocatorClient;
import net.bluemind.xivo.client.XivoClient;
import net.bluemind.xivo.common.Hosts;

public class DepDoneHandler implements Handler<AsyncResult<String>> {

	private static final Logger logger = LoggerFactory.getLogger(DepDoneHandler.class);

	private AtomicInteger handshakeCountdown = new AtomicInteger(2);

	public DepDoneHandler() {
	}

	@Override
	public void handle(AsyncResult<String> ar) {
		if (ar.failed()) {
			Throwable cause = ar.cause();
			logger.error(cause.getMessage(), cause);
		}
		logger.info("Deployement done with id: " + ar.result());
		int newValue = handshakeCountdown.decrementAndGet();
		if (newValue == 0) {
			VertxPlatform.getVertx().setTimer(10000, new Handler<Long>() {

				@Override
				public void handle(Long event) {
					handshakeDomains();
				}
			});
		}
	}

	/**
	 * 
	 */
	private void handshakeDomains() {

		final Timer locatorTimer = new Timer();
		TimerTask locatorTask = new TimerTask() {

			@Override
			public void run() {
				LocatorClient locator = new LocatorClient();
				String host = locator.locateHost("bm/core", "admin0@global.virt");
				if (host != null) {
					String url = "http://" + host + ":8090";
					IServiceProvider sp = ClientSideServiceProvider.getProvider(url, Token.admin0());
					try {
						IDomains domainApi = sp.instance(IDomains.class, InstallationId.getIdentifier());
						List<ItemValue<Domain>> domains = domainApi.all();
						XivoClient xivoClient = new XivoClient(Hosts.xivo());
						for (ItemValue<Domain> d : domains) {
							if (!d.value.global) {
								logger.info("Trying handshake for {}", d.uid);
								locatorTimer.cancel();
								xivoClient.handshake(d.uid);
							}
						}
					} catch (Exception e) {
						logger.error(e.getMessage(), e);
						logger.error("Fail to fetch domains list. retry in 5s", e);
					}
				}

			}

		};

		locatorTimer.schedule(locatorTask, 1000, 5000);

	}
}
