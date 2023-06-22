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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.system.application.registration;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.curator.shaded.com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.system.application.registration.hook.IAppStatusInfoHook;
import net.bluemind.system.application.registration.model.ApplicationInfoModel;
import net.bluemind.system.application.registration.model.ApplicationMetric;
import net.bluemind.system.application.registration.model.ApplicationMetric.AppTag;

public class ApplicationRegistration extends AbstractVerticle {

	public static final String APPLICATION_REGISTRATION = "bm.application.registration";
	public static final String APPLICATION_REGISTRATION_INIT = "bm.application.registration.init";
	public static final Logger logger = LoggerFactory.getLogger(ApplicationRegistration.class);

	private final Store store;
	private final IAppStatusInfoHook hook;

	public ApplicationRegistration(Store store, IAppStatusInfoHook hook) {
		this.store = store;
		this.hook = hook;
	}

	@Override
	public void start(Promise<Void> startPromise) throws Exception {
		vertx.eventBus().consumer(APPLICATION_REGISTRATION, event -> {
			ApplicationInfoModel info = JsonUtils.read(((JsonObject) event.body()).encode(),
					ApplicationInfoModel.class);
			logger.debug("Registering server {} -> {} -> {} -> {}", info.address, info.product, info.installationId,
					info.machineId);

			Publisher applicationPublisher = store.getPublisher(new DefaultTopicDescriptor("bluemind_cluster",
					"__nodes__", "system", "application-registration", info.product));
			applicationPublisher.store(info.product, info.machineId.getBytes(), info.toJson().getBytes());
		});

		vertx.eventBus().consumer(APPLICATION_REGISTRATION_INIT, event -> {
			ApplicationInfoModel info = ((JsonObject) event.body()).mapTo(ApplicationInfoModel.class);
			hook.updateStateAndVersion(info);
			setupReportingLoop(info);
		});

		startPromise.complete();
	}

	private void setupReportingLoop(ApplicationInfoModel info) {
		VertxPlatform.getVertx().eventBus().consumer("bm.monitoring.kafka.metrics", metric -> {
			JsonObject body = (JsonObject) metric.body();
			JsonArray jsonArray = body.getJsonArray("metrics");
			for (Object mObj : jsonArray) {
				JsonObject jObj = (JsonObject) mObj;
				if ("record-send-rate".equals(jObj.getString("key"))) {
					updateMasterMetric(info, "record-send-rate", jObj.getLong("value"));
				} else if ("lag".equals(jObj.getString("key"))) {
					updateTailMetric(info, "records-lag-max", jObj.getLong("value"));
				}
			}
		});

		VertxPlatform.getVertx().setPeriodic(TimeUnit.SECONDS.toMillis(15), id -> {
			if (!Strings.isNullOrEmpty(hook.getState()) && !Strings.isNullOrEmpty(hook.getVersion())) {
				hook.updateStateAndVersion(info);
			}
			VertxPlatform.getVertx().eventBus().publish(ApplicationRegistration.APPLICATION_REGISTRATION,
					JsonObject.mapFrom(info));
		});
	}

	private void updateTailMetric(ApplicationInfoModel info, String key, long value) {
		Optional<ApplicationMetric> ml = info.metrics.stream().filter(m -> m.tag == AppTag.TAIL && key.equals(m.key))
				.findFirst();
		if (ml.isPresent()) {
			ml.get().value = value;
		} else {
			info.metrics.add(ApplicationMetric.create(key, value, AppTag.TAIL));
		}
	}

	private void updateMasterMetric(ApplicationInfoModel info, String key, long value) {
		Optional<ApplicationMetric> ml = info.metrics.stream().filter(m -> m.tag == AppTag.MASTER && key.equals(m.key))
				.findFirst();
		if (ml.isPresent()) {
			ml.get().value = value;
		} else {
			info.metrics.add(ApplicationMetric.create(key, value, AppTag.MASTER));
		}
	}

}
