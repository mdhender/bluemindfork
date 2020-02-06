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
package net.bluemind.system.pg.internal;

import java.io.File;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Verticle;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.service.LoggingTaskMonitor;
import net.bluemind.core.task.service.NullTaskMonitor;
import net.bluemind.lib.vertx.IUniqueVerticleFactory;
import net.bluemind.lib.vertx.IVerticleFactory;
import net.bluemind.system.pg.api.IInternalPostgresMaintenance;

public class PGMaintenanceVerticle extends AbstractVerticle {

	private static final Logger logger = LoggerFactory.getLogger(PGMaintenanceVerticle.class);

	public static class Factory implements IVerticleFactory, IUniqueVerticleFactory {

		@Override
		public boolean isWorker() {
			return true;
		}

		@Override
		public Verticle newInstance() {
			return new PGMaintenanceVerticle();
		}

	}

	@Override
	public void start() {
		nextTimer();
	}

	public static void main(String[] args) {
		System.err.println(millisecTo2AM());
	}

	public static long millisecTo2AM() {
		LocalTime t2am = LocalTime.MIDNIGHT.plusHours(2);
		LocalDateTime next2AM = LocalDateTime.of(LocalDate.now(), t2am);

		if (next2AM.isBefore(LocalDateTime.now())) {
			next2AM = next2AM.plusDays(1);
		}
		return Math.max(Duration.between(LocalDateTime.now(), next2AM).get(ChronoUnit.SECONDS) * 1000, 2);
	}

	private void nextTimer() {
		vertx.setTimer(millisecTo2AM(), (i) -> {
			try {
				runPGMaintenance();
			} catch (Exception e) {
				logger.error("error execution pg maintenance", e);
			}
			nextTimer();
		});
	}

	private void runPGMaintenance() {
		if (isDisabled()) {
			logger.warn("pgmaintenance is disabled");
			return;
		}

		BmContext context = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).getContext();
		context.provider().instance(IInternalPostgresMaintenance.class)
				.executeMaintenanceQueries(new LoggingTaskMonitor(logger, new NullTaskMonitor(), 0));
	}

	protected boolean isDisabled() {
		return new File(System.getProperty("user.home") + "/no.pgmaintenance").exists();
	}

}
