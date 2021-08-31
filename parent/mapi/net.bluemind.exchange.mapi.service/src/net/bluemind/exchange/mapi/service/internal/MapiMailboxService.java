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
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.json.JsonObject;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.ContainerQuery;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemUri;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.persistence.DataSourceRouter;
import net.bluemind.core.jdbc.JdbcAbstractStore;
import net.bluemind.core.rest.BmContext;
import net.bluemind.exchange.mapi.api.IMapiFolderAssociatedInformation;
import net.bluemind.exchange.mapi.api.IMapiMailbox;
import net.bluemind.exchange.mapi.api.MapiFAIContainer;
import net.bluemind.exchange.mapi.api.MapiReplica;
import net.bluemind.exchange.mapi.hook.IMapiArtifactsHook;
import net.bluemind.exchange.mapi.hook.MapiArtifactsHooks;
import net.bluemind.exchange.mapi.persistence.MapiReplicaStore;
import net.bluemind.exchange.publicfolders.common.PublicFolders;
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

	private boolean pfMailbox;

	public MapiMailboxService(BmContext context, String domainUid, String mailboxUid) throws ServerFault {
		this.context = context;
		this.domainUid = domainUid;
		this.mailboxUid = mailboxUid;
		this.mapiReplicaStore = new MapiReplicaStore(context.getDataSource());
		this.pfMailbox = mailboxUid.equals(PublicFolders.mailboxGuid(domainUid));
	}

	@Override
	public void create(MapiReplica replica) throws ServerFault {
		replica.mailboxUid = this.mailboxUid;
		try {
			mapiReplicaStore.store(replica);
			for (IMapiArtifactsHook h : MapiArtifactsHooks.get()) {
				h.onReplicaStored(domainUid, replica);
			}
		} catch (SQLException e1) {
			throw ServerFault.sqlFault(e1);
		}
		checkFaiContainer(replica);

	}

	private void checkFaiContainer(MapiReplica replica) {
		String faiContainerId = MapiFAIContainer.getIdentifier(replica);
		DataSourceRouter.invalidateContainer(faiContainerId);
		ContainerDescriptor fais = ContainerDescriptor.create(faiContainerId, faiContainerId, mailboxUid,
				MapiFAIContainer.TYPE, domainUid, true);
		IContainers contApi = context.su().provider().instance(IContainers.class);
		ContainerDescriptor current = contApi.getIfPresent(faiContainerId);
		if (current != null && !current.owner.equals(fais.owner)) {
			logger.info("Reset FAI container {} as owner is wrong", faiContainerId);
			contApi.delete(faiContainerId);
			current = null;
		}
		if (current == null) {
			contApi.create(faiContainerId, fais);
			logger.info("Created FAI container {}", faiContainerId);
		}
		if (pfMailbox) {
			logger.info("Setting domain-wide {} ACLs for PF FAI folder {}", domainUid, faiContainerId);
			IContainerManagement mgmtApi = context.su().provider().instance(IContainerManagement.class, faiContainerId);
			mgmtApi.setAccessControlList(Arrays.asList(AccessControlEntry.create(domainUid, Verb.All)));
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

		JsonObject msg = new JsonObject().put("product", "bm-mapi").put("user", latd).put("enabled", enable);
		logger.info("Reconfiguring logs for {}, enable: {}", latd, enable);
		MQ.getProducer(Topic.LOGBACK_CONFIG).send(msg);
	}

}
