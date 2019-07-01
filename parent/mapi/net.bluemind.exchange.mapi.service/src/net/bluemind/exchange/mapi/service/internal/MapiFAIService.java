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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.json.JsonObject;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.ItemVersion;
import net.bluemind.core.container.persistance.ContainerStore;
import net.bluemind.core.container.persistance.DataSourceRouter;
import net.bluemind.core.container.service.internal.ContainerStoreService;
import net.bluemind.core.rest.BmContext;
import net.bluemind.exchange.mapi.api.IMapiFolderAssociatedInformation;
import net.bluemind.exchange.mapi.api.MapiFAI;
import net.bluemind.exchange.mapi.api.MapiFAIContainer;
import net.bluemind.exchange.mapi.persistence.MapiFAIStore;

public class MapiFAIService implements IMapiFolderAssociatedInformation {

	private static final Logger logger = LoggerFactory.getLogger(MapiFAIService.class);

	private final BmContext context;
	private final ContainerStoreService<MapiFAI> storeService;
	private final MapiFAIStore mapiFaiStore;
	private final String localReplicaGuid;
	private final Container container;

	public MapiFAIService(BmContext context, String localReplicaGuid, Container faiContainer) {
		this.context = context;
		this.container = faiContainer;
		this.localReplicaGuid = localReplicaGuid;
		DataSource ds = DataSourceRouter.get(context, faiContainer.uid);
		this.mapiFaiStore = new MapiFAIStore(ds, faiContainer);
		this.storeService = new ContainerStoreService<>(ds, context.getSecurityContext(), faiContainer,
				MapiFAIContainer.TYPE, mapiFaiStore);

	}

	@Override
	public Collection<Long> deleteByIds(Collection<Long> internalIds) {
		List<Long> deleted = new ArrayList<>(internalIds.size());
		for (long itemId : internalIds) {
			ItemVersion done = storeService.delete(faiUid(itemId));
			if (done != null) {
				deleted.add(itemId);
			}
		}
		return deleted;

	}

	private String faiUid(long internalId) {
		return localReplicaGuid + "-" + Long.toHexString(internalId);
	}

	@Override
	public ItemValue<MapiFAI> store(long internalId, MapiFAI fai) throws ServerFault {
		String pidTagSourceKey = faiUid(internalId);
		String extId = pidTagSourceKey;
		ItemValue<MapiFAI> existingFAI = storeService.getByExtId(extId);
		if (existingFAI == null) {
			extId = getPreloadExtId(fai);
			if (extId != null) {
				existingFAI = storeService.getByExtId(extId);
			}
		}
		boolean update = false;
		String uid;
		if (existingFAI == null) {
			storeService.createWithId(pidTagSourceKey, internalId, pidTagSourceKey, pidTagSourceKey, fai);
			uid = pidTagSourceKey;
		} else {
			storeService.update(extId, pidTagSourceKey, fai);
			uid = extId;
			update = true;
		}
		logger.info("[{}] fai {}.", context.getSecurityContext().getSubject(), update ? "UPDATED" : "CREATED");
		return storeService.getByExtId(uid);
	}

	@Override
	public void preload(MapiFAI fai) throws ServerFault {
		String pidTagSourceKey = getPreloadExtId(fai);
		if (pidTagSourceKey != null) {
			ItemValue<MapiFAI> existingFAI = storeService.getByExtId(pidTagSourceKey);
			if (existingFAI == null) {
				storeService.create(pidTagSourceKey, pidTagSourceKey, pidTagSourceKey, fai);
			}
		}
	}

	@Override
	public List<ItemValue<MapiFAI>> all() {
		return storeService.all();
	}

	@Override
	public List<ItemValue<MapiFAI>> getByFolderId(String folderId) throws ServerFault {
		try {
			List<String> found = mapiFaiStore.byFolder(folderId);
			logger.info("Load FAIs for folder {} => {}", folderId, found.size());
			return storeService.getMultiple(found);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	private String getPreloadExtId(MapiFAI fai) {
		JsonObject js = new JsonObject(fai.faiJson);
		JsonObject props = js.getObject("setProperties");
		if (props != null) {
			String mClass = props.getString("PidTagMessageClass");
			return localReplicaGuid + "-" + "preload-" + mClass;
		}
		return null;
	}

	@Override
	public void deleteAll() throws ServerFault {
		storeService.deleteAll();
	}

	@Override
	public void xfer(String serverUid) throws ServerFault {

		DataSource ds = context.getMailboxDataSource(serverUid);
		ContainerStore cs = new ContainerStore(null, ds, context.getSecurityContext());
		Container c;
		try {
			c = cs.get(container.uid);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}

		storeService.xfer(ds, c, new MapiFAIStore(ds, c));

	}

}
