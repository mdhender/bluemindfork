/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.exchange.mapi.service.internal;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.json.JsonObject;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.Ack;
import net.bluemind.core.container.api.Count;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerChangelog;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.ItemChangelog;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.ItemVersion;
import net.bluemind.core.container.model.SortDescriptor;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.service.internal.ContainerStoreService;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.rest.BmContext;
import net.bluemind.exchange.mapi.api.IMapiFolder;
import net.bluemind.exchange.mapi.api.MapiRawMessage;
import net.bluemind.exchange.mapi.persistence.MapiRawMessageStore;

public class MapiFolderService implements IMapiFolder {

	private static final Logger logger = LoggerFactory.getLogger(MapiFolderService.class);

	private final BmContext context;
	private final Container container;
	private final MapiRawMessageStore mapiRawMessageStore;
	private final ContainerStoreService<MapiRawMessage> storeService;
	private final RBACManager rbacManager;

	public MapiFolderService(BmContext ctx, DataSource ds, Container c) {
		context = ctx;
		container = c;
		this.mapiRawMessageStore = new MapiRawMessageStore(ds, c);
		this.storeService = new ContainerStoreService<>(ds, ctx.getSecurityContext(), c, mapiRawMessageStore);
		rbacManager = RBACManager.forContext(ctx).forContainer(c);
	}

	@Override
	public ItemValue<MapiRawMessage> getCompleteById(long id) {
		rbacManager.check(Verb.Read.name());

		return storeService.get(id, null);
	}

	@Override
	public Ack updateById(long id, MapiRawMessage value) {
		rbacManager.check(Verb.Write.name());

		String dn = displayName(id, value);
		ItemVersion iv = storeService.update(id, dn, value);
		return Ack.create(iv.version);
	}

	private String displayName(long id, MapiRawMessage value) {
		JsonObject js = new JsonObject(Optional.ofNullable(value.contentJson).orElse("{}"));
		JsonObject props = js.getJsonObject("setProperties");
		String dn = "mapi-raw:" + id;
		if (props != null) {
			if (props.containsKey("PidTagDisplayName")) {
				dn = js.getString("PidTagDisplayName");
			} else if (props.containsKey("PidTagNormalizedSubject")) {
				dn = js.getString("PidTagNormalizedSubject");
			} else if (props.containsKey("PidTagConversationTopic")) {
				dn = js.getString("PidTagConversationTopic");
			}
		}
		return dn;
	}

	private String uid(long id, MapiRawMessage value) {
		logger.debug("value is unused: {}", value);
		return "mapi-raw-msg:" + id;
	}

	@Override
	public Ack createById(long id, MapiRawMessage value) {
		rbacManager.check(Verb.Write.name());

		ItemVersion created = storeService.createWithId(uid(id, value), id, null, displayName(id, value), value);
		return Ack.create(created.version);
	}

	@Override
	public void deleteById(long id) {
		rbacManager.check(Verb.Write.name());
		storeService.delete(id);
	}

	@Override
	public ItemChangelog itemChangelog(String itemUid, Long since) throws ServerFault {
		rbacManager.check(Verb.Read.name());
		return storeService.changelog(itemUid, since, Long.MAX_VALUE);
	}

	@Override
	public ContainerChangelog containerChangelog(Long since) throws ServerFault {
		rbacManager.check(Verb.Read.name());
		return storeService.changelog(since, Long.MAX_VALUE);
	}

	@Override
	public ContainerChangeset<String> changeset(Long since) throws ServerFault {
		rbacManager.check(Verb.Read.name());
		return storeService.changeset(since, Long.MAX_VALUE);
	}

	@Override
	public ContainerChangeset<Long> changesetById(Long since) throws ServerFault {
		rbacManager.check(Verb.Read.name());
		return storeService.changesetById(since, Long.MAX_VALUE);
	}

	@Override
	public ContainerChangeset<ItemVersion> filteredChangesetById(Long since, ItemFlagFilter filter) throws ServerFault {
		rbacManager.check(Verb.Read.name());
		return storeService.changesetById(since, filter);
	}

	@Override
	public long getVersion() throws ServerFault {
		rbacManager.check(Verb.Read.name());
		return storeService.getVersion();
	}

	@Override
	public Count count(ItemFlagFilter filter) throws ServerFault {
		rbacManager.check(Verb.Read.name());
		return storeService.count(filter);
	}

	@Override
	public void reset() {
		rbacManager.check(Verb.Write.name(), Verb.Manage.name());
		storeService.prepareContainerDelete();
	}

	@Override
	public List<Long> sortedIds(SortDescriptor sorted) throws ServerFault {
		rbacManager.check(Verb.Read.name());
		try {
			return mapiRawMessageStore.sortedIds(sorted);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
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
		storeService.xfer(ds, c, new MapiRawMessageStore(ds, c));
	}

	@Override
	public void multipleDeleteById(List<Long> ids) throws ServerFault {
		rbacManager.check(Verb.Write.name());
		ids.forEach(id -> storeService.delete(id));
	}

	@Override
	public List<ItemValue<MapiRawMessage>> multipleGetById(List<Long> ids) {
		rbacManager.check(Verb.Read.name());
		return storeService.getMultipleById(ids);
	}

}
