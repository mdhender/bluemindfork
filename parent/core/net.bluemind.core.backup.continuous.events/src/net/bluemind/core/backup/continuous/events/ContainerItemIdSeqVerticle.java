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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.core.backup.continuous.events;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Verticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import net.bluemind.core.backup.continuous.DefaultBackupStore;
import net.bluemind.core.backup.continuous.dto.ContainerItemIdSeq;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.lib.vertx.IVerticleFactory;
import net.bluemind.system.api.SystemState;

public class ContainerItemIdSeqVerticle extends AbstractVerticle {
	private static final Logger logger = LoggerFactory.getLogger(ContainerItemIdSeqVerticle.class);
	private static final String TYPE = "container_item_id_seq";

	public static class Factory implements IVerticleFactory {

		@Override
		public boolean isWorker() {
			return true;
		}

		@Override
		public Verticle newInstance() {
			return new ContainerItemIdSeqVerticle();
		}

	}

	@Override
	public void start(Promise<Void> startPromise) throws Exception {
		MessageConsumer<JsonObject> stateConsumer = vertx.eventBus().consumer(SystemState.BROADCAST);
		stateConsumer.handler(m -> {
			SystemState state = SystemState.fromOperation(m.body().getString("operation"));
			if (state == SystemState.CORE_STATE_RUNNING) {
				stateConsumer.unregister();
				vertx.setPeriodic(TimeUnit.MINUTES.toMillis(1), timerId -> {
					ContainerItemIdSeq itemIdSeq = fetchContainerItemIdSeq();
					backupContainerItemIdSeq(itemIdSeq);
				});
			}
		});
		startPromise.complete();
	}

	private ContainerItemIdSeq fetchContainerItemIdSeq() {
		ContainerItemIdSeq maxItemIds = new ContainerItemIdSeq();
		maxItemIds.defaultDataSourceSeq = sequence(ServerSideServiceProvider.defaultDataSource);
		ServerSideServiceProvider.mailboxDataSource.forEach(
				(serverUid, dataSource) -> maxItemIds.mailboxDataSourceSeq.put(serverUid, sequence(dataSource)));
		return maxItemIds;
	}

	private long sequence(DataSource dataSource) {
		try (Connection connection = dataSource.getConnection(); Statement stmt = connection.createStatement()) {
			ResultSet result = stmt.executeQuery("select last_value as seq from t_container_item_id_seq");
			result.next();
			return result.getLong("seq");
		} catch (SQLException e) {
			logger.error("Unable to fetch max id from t_container_item", e);
			return 0;
		}
	}

	private void backupContainerItemIdSeq(ContainerItemIdSeq itemIdSeq) {
		ContainerDescriptor metaDesc = ContainerDescriptor.create(TYPE, TYPE, "system", TYPE, null, true);
		ItemValue<ContainerItemIdSeq> metaItem = ItemValue.create(TYPE, itemIdSeq);
		metaItem.internalId = metaItem.uid.hashCode();
		DefaultBackupStore.store().<ContainerItemIdSeq>forContainer(metaDesc).store(metaItem);
	}

}
