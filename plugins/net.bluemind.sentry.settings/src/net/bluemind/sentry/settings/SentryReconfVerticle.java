/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.sentry.settings;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.sentry.Sentry;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Verticle;
import io.vertx.core.json.JsonObject;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.Topic;
import net.bluemind.lib.vertx.IUniqueVerticleFactory;
import net.bluemind.lib.vertx.IVerticleFactory;

public class SentryReconfVerticle extends AbstractVerticle {
	private static final Logger logger = LoggerFactory.getLogger(SentryReconfVerticle.class);

	public static class Reg implements IVerticleFactory, IUniqueVerticleFactory {
		@Override
		public boolean isWorker() {
			return true;
		}

		@Override
		public Verticle newInstance() {
			return new SentryReconfVerticle();
		}

	}

	@Override
	public void start() {
		MQ.init().thenAccept(v -> {
			MQ.registerConsumer(Topic.SENTRY_CONFIG, msg -> {
				JsonObject js = msg.toJson();
				String sentryDsn = js.getString("dsn", "");
				String sentryWebDsn = js.getString("webdsn", "");
				String environment = js.getString("environment", "BM_COMMUNITY");
				String release = js.getString("release", "UNKNOWN_RELEASE");
				String servername = js.getString("servername", "UNKNOWN_SERVER");
				SentryProperties sentryProps = new SentryProperties();
				sentryProps.setDsn(sentryDsn);
				sentryProps.setWebDsn(sentryWebDsn);
				sentryProps.setEnvironment(environment);
				sentryProps.setRelease(release);
				sentryProps.setServerName(servername);
				try {
					sentryProps.update();
				} catch (IOException ioe) {
					logger.error("Unable to update sentry properties: {}", ioe.getMessage(), ioe);
				}
				ClientAccess.setSettings(sentryProps);
				Sentry.close();
				if (sentryProps.enabled()) {
					logger.info("Sentry enable");
					Sentry.init(options -> {
						options.setEnableExternalConfiguration(true);
					});
				}
			});
			logger.info("Waiting for sentry re-configuration orders on topic {}", Topic.SENTRY_CONFIG);
		});
	}

}
