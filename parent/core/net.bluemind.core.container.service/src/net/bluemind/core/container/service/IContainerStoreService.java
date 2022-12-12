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
package net.bluemind.core.container.service;

import java.util.List;

import javax.sql.DataSource;

import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerChangelog;
import net.bluemind.core.container.model.IdQuery;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemChangelog;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.ItemVersion;
import net.bluemind.core.container.persistence.IItemValueStore;

public interface IContainerStoreService<T> {

	public ContainerChangelog changelog(Long from, long to);

	public ItemChangelog changelog(String itemUid, Long from, long to);

	public ItemValue<T> get(String uid, Long version);

	ItemValue<T> get(long id, Long version);

	/**
	 * Get item by external ID
	 * 
	 * @param extId
	 * @return null if the given external id is not found
	 * @throws ServerFault
	 */
	public ItemValue<T> getByExtId(String extId);

	/**
	 * Create item without external ID
	 * 
	 * @param uid
	 * @param displayName
	 * @param value
	 * @throws ServerFault
	 */
	ItemVersion create(String uid, String displayName, T value);

	/**
	 * Create item from an existing one
	 * 
	 * @param item
	 * @param displayName
	 * @param value
	 * @throws ServerFault
	 */
	ItemVersion create(Item item, T value);

	public void attach(String uid, String displayName, T value);

	/**
	 * Create item with external ID
	 * 
	 * @param uid
	 * @param extId
	 * @param displayName
	 * @param value
	 * @throws ServerFault
	 */
	ItemVersion create(String uid, String extId, String displayName, T value);

	/**
	 * Create item with external and a given internal ID.
	 * 
	 * Use this only if you are sure that the internal ID is available (because you
	 * pre-allocated some ids).
	 * 
	 * If internalId is null, this is identical to create.
	 * 
	 * @param uid
	 * @param internalId
	 * @param extId
	 * @param displayName
	 * @param value
	 * @throws ServerFault
	 */
	ItemVersion createWithId(String uid, Long internalId, String extId, String displayName, T value);

	ItemVersion update(String uid, String displayName, T value);

	ItemVersion update(long id, String displayName, T value);

	ItemVersion update(Item item, String displayName, T value);

	/**
	 * @param uid
	 * @return the id & version of the deleted item or null if nothing was deleted
	 * @throws ServerFault
	 */
	ItemVersion delete(String uid);

	/**
	 * @param id
	 * @return the id & version of the deleted item or null if nothing was deleted
	 * @throws ServerFault
	 */
	ItemVersion delete(long id);

	public void detach(String uid);

	/**
	 * Delete all values (changelog is available for deleted values).
	 * 
	 * Consider {@link IContainerStoreService#prepareContainerDelete())} when
	 * container deletion is your next step.
	 * 
	 * @throws ServerFault
	 */
	public void deleteAll();

	public ItemVersion touch(String uid);

	public List<String> allUids();

	ListResult<Long> allIds(IdQuery query);

	/**
	 * Delete all values. Prefer it to {@link IContainerStoreService#deleteAll()}
	 * when you want to drop the container after the call.
	 * 
	 * @throws ServerFault
	 */
	public void prepareContainerDelete();

	public long setExtId(String uid, String extId);

	public void xfer(DataSource targetDataSource, Container targetContainer, IItemValueStore<T> targetItemValueStore);
}
