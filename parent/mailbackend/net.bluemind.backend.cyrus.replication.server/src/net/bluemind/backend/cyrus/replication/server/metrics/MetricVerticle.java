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
package net.bluemind.backend.cyrus.replication.server.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.platform.Verticle;

import net.bluemind.backend.cyrus.replication.server.ReplicationSession;
import net.bluemind.lib.vertx.IUniqueVerticleFactory;
import net.bluemind.lib.vertx.IVerticleFactory;

public class MetricVerticle extends Verticle {

	private static final Logger logger = LoggerFactory.getLogger(MetricVerticle.class);

	public static class Factory implements IVerticleFactory, IUniqueVerticleFactory {

		@Override
		public boolean isWorker() {
			return true;
		}

		@Override
		public Verticle newInstance() {
			return new MetricVerticle();
		}

	}

	public void start() {
		vertx.setPeriodic(10000, tid -> {
			long active = ReplicationSession.activeSessions.get();
			if (active > 0) {
				logger.info("{} active replication connection(s)", active);
			} else {
				logger.warn("NO active replication connection, consider restarting cyrus ?");
			}
		});
	}

}
