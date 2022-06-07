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
package net.bluemind.webappdata.service.internal;

import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.Ack;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerChangelog;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemChangelog;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.ItemVersion;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.service.ChangeLogUtil;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.rest.BmContext;
import net.bluemind.webappdata.api.IWebAppData;
import net.bluemind.webappdata.api.WebAppData;

/**
 * 
 * WebAppData API.
 * 
 */
public class WebAppDataService implements IWebAppData {
	private static final Logger logger = LoggerFactory.getLogger(WebAppDataService.class);
	private BmContext bmContext;
	private Container container;
	private RBACManager rbacManager;
	private WebAppDataValidator validator;
	private WebAppDataStoreService storeService;

	public WebAppDataService(DataSource pool, Container container, BmContext bmContext) {
		this.bmContext = bmContext;
		this.container = container;
		this.rbacManager = RBACManager.forContext(bmContext).forContainer(container);
		this.storeService = new WebAppDataStoreService(pool, bmContext.getSecurityContext(), container);
		validator = new WebAppDataValidator();
	}

	@Override
	public ItemChangelog itemChangelog(String itemUid, Long since) throws ServerFault {
		rbacManager.check(Verb.Read.name());
		return ChangeLogUtil.getItemChangeLog(itemUid, since, bmContext, storeService, container.domainUid);
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
	public void xfer(String serverUid) throws ServerFault {
		rbacManager.check(Verb.Write.name());
		// TODO Auto-generated method stub

	}

	@Override
	public Ack update(String uid, WebAppData webAppData) {
		rbacManager.check(Verb.Write.name());
		WebAppData old = get(uid);
		if (old == null) {
			logger.warn(
					"trying to update WebAppData item with uid " + uid + " while it doesnt exist. Fallback on create.");
			return create(uid, webAppData);
		}
		validator.update(old, webAppData);
		Item item = Item.create(uid, null);
		ItemVersion version = storeService.update(item, webAppData.key, webAppData);
		return Ack.create(version.version);
	}

	@Override
	public Ack create(String uid, WebAppData webAppData) {
		rbacManager.check(Verb.Write.name());
		WebAppData existingKey = webAppData == null ? null : getByKey(webAppData.key);
		validator.create(webAppData, existingKey);
		Item item = Item.create(uid, null);
		ItemVersion version = storeService.create(item, webAppData);
		return Ack.create(version.version);
	}

	@Override
	public void delete(String uid) {
		rbacManager.check(Verb.Write.name());
		if (get(uid) == null) {
			throw new ServerFault("item doesnt exists", ErrorCode.NOT_FOUND);
		} else {
			storeService.delete(uid);
		}
	}

	@Override
	public ItemValue<WebAppData> getComplete(String uid) {
		rbacManager.check(Verb.Read.name());
		return storeService.get(uid, null);
	}

	@Override
	public List<ItemValue<WebAppData>> multipleGet(List<String> uids) {
		rbacManager.check(Verb.Read.name());
		return storeService.getMultiple(uids);
	}

	@Override
	public WebAppData get(String uid) {
		ItemValue<WebAppData> itemValue = this.getComplete(uid);
		return itemValue == null ? null : itemValue.value;
	}

	@Override
	public void restore(ItemValue<WebAppData> item, boolean isCreate) {
		rbacManager.check(Verb.Write.name());
		// TODO Auto-generated method stub
	}

	@Override
	public ItemValue<WebAppData> getCompleteById(long id) {
		rbacManager.check(Verb.Read.name());
		return storeService.get(id, null);
	}

	@Override
	public List<ItemValue<WebAppData>> multipleGetById(List<Long> ids) {
		rbacManager.check(Verb.Read.name());
		return storeService.getMultipleById(ids);
	}

	@Override
	public List<String> allUids() {
		rbacManager.check(Verb.Read.name());
		return storeService.allUids();
	}

	@Override
	public void deleteAll() {
		rbacManager.check(Verb.Write.name());
		storeService.deleteAll();
	}

	@Override
	public WebAppData getByKey(String key) {
		rbacManager.check(Verb.Read.name());
		return storeService.getByKey(key);
	}

}
