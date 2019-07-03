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
package net.bluemind.exchange.mapi.service.internal;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.json.JsonObject;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.ContainerQuery;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemUri;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.jdbc.JdbcAbstractStore;
import net.bluemind.core.rest.BmContext;
import net.bluemind.exchange.mapi.api.IMapiFolderAssociatedInformation;
import net.bluemind.exchange.mapi.api.IMapiMailbox;
import net.bluemind.exchange.mapi.api.MapiFAIContainer;
import net.bluemind.exchange.mapi.api.MapiReplica;
import net.bluemind.exchange.mapi.persistence.MapiReplicaStore;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.Topic;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public class MapiMailboxService implements IMapiMailbox {

	private static final Logger logger = LoggerFactory.getLogger(MapiMailboxService.class);

	private BmContext context;
	private MapiReplicaStore mapiReplicaStore;

	private String domainUid;

	private String mailboxUid;

	public MapiMailboxService(BmContext context, String domainUid, String mailboxUid) throws ServerFault {
		this.context = context;
		this.domainUid = domainUid;
		this.mailboxUid = mailboxUid;
		this.context = context;
		this.mapiReplicaStore = new MapiReplicaStore(context.getDataSource());
	}

	@Override
	public void create(MapiReplica replica) throws ServerFault {
		replica.mailboxUid = this.mailboxUid;
		try {
			mapiReplicaStore.store(replica);

			String faiContainerId = MapiFAIContainer.getIdentifier(replica);
			ContainerDescriptor fais = ContainerDescriptor.create(faiContainerId, faiContainerId,
					context.getSecurityContext().getSubject(), MapiFAIContainer.TYPE, domainUid, true);
			IContainers contApi = context.provider().instance(IContainers.class);
			contApi.create(faiContainerId, fais);
			logger.info("Created container {}", faiContainerId);
		} catch (SQLException e1) {
			throw ServerFault.sqlFault(e1);
		}

	}

	@Override
	public MapiReplica get() throws ServerFault {
		try {
			return mapiReplicaStore.get(mailboxUid);
		} catch (SQLException e) {
			throw new ServerFault(e);
		}
	}

	@Override
	public ItemUri locate(long itemId) {
		return JdbcAbstractStore.doOrFail(() -> {
			try (Connection con = context.getDataSource().getConnection(); Statement st = con.createStatement()) {
				String containerUid = null;
				String itemUid = null;
				try (ResultSet rs = st.executeQuery(
						"select c.uid, ci.uid from t_container_item ci inner join t_container c on ci.container_id=c.id where ci.id="
								+ itemId)) {
					if (rs.next()) {
						containerUid = rs.getString(1);
						itemUid = rs.getString(2);
					} else {
						throw ServerFault.notFound("itemid " + itemId + " not found.");
					}
				}
				return ItemUri.create(containerUid, itemUid);
			}
		});
	}

	@Override
	public void delete() throws ServerFault {
		MapiReplica replica = get();
		if (replica == null) {
			logger.info("Replica for {} is missing which is fine as we want to delete it.", mailboxUid);
			return;
		}
		try {
			IMapiFolderAssociatedInformation faiService = context.su().provider()
					.instance(IMapiFolderAssociatedInformation.class, replica.localReplicaGuid);
			faiService.deleteAll();
			IContainers contApi = context.su().provider().instance(IContainers.class);
			List<ContainerDescriptor> faiContainer = contApi
					.all(ContainerQuery.ownerAndType(mailboxUid, MapiFAIContainer.TYPE));
			for (ContainerDescriptor containerDescriptor : faiContainer) {
				contApi.delete(containerDescriptor.uid);
			}
			mapiReplicaStore.delete(mailboxUid);
		} catch (SQLException e) {
			throw new ServerFault(e);
		}
	}

	@Override
	public void enablePerUserLog(boolean enable) {
		IUser userApi = context.provider().instance(IUser.class, domainUid);
		ItemValue<User> asUser = userApi.getComplete(mailboxUid);
		String latd = asUser.value.login + "@" + domainUid;

		JsonObject msg = new JsonObject().putString("product", "bm-mapi").putString("user", latd).putBoolean("enabled",
				enable);
		logger.info("Reconfiguring logs for {}, enable: {}", latd, enable);
		MQ.getProducer(Topic.LOGBACK_CONFIG).send(msg);
	}

}
