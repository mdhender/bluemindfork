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
package net.bluemind.metrics.core.healing;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Verticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import net.bluemind.lib.vertx.IUniqueVerticleFactory;
import net.bluemind.lib.vertx.IVerticleFactory;
import net.bluemind.metrics.core.Product;
import net.bluemind.metrics.core.tick.TickTemplateHelper;
import net.bluemind.metrics.core.tick.TickTemplateHelper.AlertId;

public class MailspoolWorstResponseTimeVerticle extends AbstractVerticle {

	private static final Logger logger = LoggerFactory.getLogger(MailspoolWorstResponseTimeVerticle.class);

	public static class Factory implements IVerticleFactory, IUniqueVerticleFactory {

		@Override
		public boolean isWorker() {
			return true;
		}

		@Override
		public Verticle newInstance() {
			return new MailspoolWorstResponseTimeVerticle();
		}
	}

	@Override
	public void start() {
		vertx.eventBus().consumer("kapacitor.alert", (Message<JsonObject> msg) -> {
			JsonObject obj = msg.body();
			String newLevel = obj.getString("level");
			if ("OK".equals(newLevel)) {
				return;
			}
			String idStr = obj.getString("id");
			Optional<AlertId> id = TickTemplateHelper.idFromString(idStr);
			if (id.isPresent() && id.get().alertSubId.contains("mailspool-worst-response-time")
					&& id.get().product.name.equals(Product.ES.name)) {
				logger.info("Elasticsearch: work needed on {}", id.get().datalocation);
			}
		});
	}

}
