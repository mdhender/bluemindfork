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
package net.bluemind.metrics.core.service;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.spectator.api.Registry;

import io.vertx.core.json.JsonObject;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.ITasksManager;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.Topic;
import net.bluemind.metrics.alerts.api.CheckResult;
import net.bluemind.metrics.alerts.api.CheckResult.Level;
import net.bluemind.metrics.alerts.api.IProductChecks;
import net.bluemind.metrics.registry.IdFactory;
import net.bluemind.metrics.registry.MetricsRegistry;

public class ProductChecksService implements IProductChecks {
	private static final Logger logger = LoggerFactory.getLogger(ProductChecksService.class);

	private static final Map<String, Optional<CheckResult>> results = new ConcurrentHashMap<>();

	static void initConsumer() {
		MQ.init().whenComplete((v, ex) -> {

			if (ex != null) {
				logger.error(ex.getMessage(), ex);
				return;
			}
			Registry reg = MetricsRegistry.get();
			IdFactory idf = new IdFactory("product.check", reg, ProductChecksService.class);

			MQ.registerConsumer(Topic.PRODUCT_CHECK_RESULTS, msg -> {
				JsonObject msgJs = msg.toJson();
				String validator = msgJs.getString("validator");
				String origin = msgJs.getString("origin");
				CheckResult cr = new CheckResult();
				boolean valid = msgJs.getBoolean("valid");
				boolean blocking = msgJs.getBoolean("blocking", false);
				if (valid) {
					cr.level = Level.OK;
				} else if (!blocking) {
					cr.level = Level.WARN;
				} else {
					cr.level = Level.CRIT;
				}
				cr.message = msgJs.getString("message");
				switch (cr.level) {
				case OK:
					logger.info("[{}@{}] Status {}", validator, origin, cr.level);
					break;
				case WARN:
					reg.counter(idf.name("warning").withTag("validator", validator)).increment();
					logger.warn("[{}@{}] Status {} ({})", validator, origin, cr.level, cr.message);
					break;
				case CRIT:
					reg.counter(idf.name("critical").withTag("validator", validator)).increment();
					logger.error("[{}@{}] Status {} ({})", validator, origin, cr.level, cr.message);
					break;
				case UNKNOWN:
				default:
					break;
				}
				results.put(validator, Optional.of(cr));
			});

		});
	}

	private final BmContext context;

	public ProductChecksService(BmContext context) {
		this.context = context;
	}

	@Override
	public Set<String> availableChecks() {
		return results.keySet();
	}

	@Override
	public CheckResult lastResult(String checkName) {
		return results.computeIfAbsent(checkName, k -> Optional.empty()).orElse(null);
	}

	@Override
	public TaskRef check(String checkName) {
		ITasksManager tm = context.provider().instance(ITasksManager.class);

		// this will help tracking when the new results becomes available
		results.remove(checkName);

		logger.info("Trigger execution of {}", checkName);
		return tm.run(monitor -> {
			monitor.begin(1, "Starting " + checkName + " check...");
			MQ.init().whenComplete((v, ex) -> {
				monitor.progress(1, "...");
				if (ex != null) {
					monitor.end(false, "check request failed: " + ex.getMessage(), "{}");
				} else {
					MQ.getProducer(Topic.PRODUCT_CHECK_REQUESTS).send(new JsonObject().put("validator", checkName));
					monitor.end(true, "check was requested", "{}");
				}

			});
		});
	}

	public static class Facto implements ServerSideServiceProvider.IServerSideServiceFactory<IProductChecks> {

		@Override
		public Class<IProductChecks> factoryClass() {
			return IProductChecks.class;
		}

		@Override
		public IProductChecks instance(BmContext context, String... params) throws ServerFault {
			return new ProductChecksService(context);
		}

	}

}
