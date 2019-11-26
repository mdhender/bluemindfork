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

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerChangelog;
import net.bluemind.core.container.model.ItemChangelog;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.ItemVersion;
import net.bluemind.core.container.persistence.IItemValueStore;

public interface IContainerStoreService<T> {

	public ContainerChangelog changelog(Long from, long to) throws ServerFault;

	public ItemChangelog changelog(String itemUid, Long from, long to) throws ServerFault;

	public ItemValue<T> get(String uid, Long version) throws ServerFault;

	ItemValue<T> get(long id, Long version) throws ServerFault;

	/**
	 * Get item by external ID
	 * 
	 * @param extId
	 * @return null if the given external id is not found
	 * @throws ServerFault
	 */
	public ItemValue<T> getByExtId(String extId) throws ServerFault;

	/**
	 * Create item without external ID
	 * 
	 * @param uid
	 * @param displayName
	 * @param value
	 * @throws ServerFault
	 */
	ItemVersion create(String uid, String displayName, T value) throws ServerFault;

	public void attach(String uid, String displayName, T value) throws ServerFault;

	/**
	 * Create item with external ID
	 * 
	 * @param uid
	 * @param extId
	 * @param displayName
	 * @param value
	 * @throws ServerFault
	 */
	ItemVersion create(String uid, String extId, String displayName, T value) throws ServerFault;

	/**
	 * Create item with external and a given internal ID.
	 * 
	 * Use this only if you are sure that the internal ID is available (because
	 * you pre-allocated some ids).
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
	ItemVersion createWithId(String uid, Long internalId, String extId, String displayName, T value) throws ServerFault;

	ItemVersion update(String uid, String displayName, T value) throws ServerFault;

	ItemVersion update(long id, String displayName, T value) throws ServerFault;

	/**
	 * @param uid
	 * @return the id & version of the deleted item or null if nothing was
	 *         deleted
	 * @throws ServerFault
	 */
	ItemVersion delete(String uid) throws ServerFault;

	/**
	 * @param id
	 * @return the id & version of the deleted item or null if nothing was
	 *         deleted
	 * @throws ServerFault
	 */
	ItemVersion delete(long id) throws ServerFault;

	public void detach(String uid) throws ServerFault;

	/**
	 * delete all values ( changelog is available for deleted values )
	 * 
	 * @throws ServerFault
	 */
	public void deleteAll() throws ServerFault;

	public void touch(String uid) throws ServerFault;

	public List<String> allUids() throws ServerFault;

	/**
	 * delete all values. To use before deleting containers
	 * 
	 * @throws ServerFault
	 */
	public void prepareContainerDelete() throws ServerFault;

	public List<String> allUidsOrderedByDisplayname() throws ServerFault;

	public long setExtId(String uid, String extId) throws ServerFault;

	public void xfer(DataSource targetDataSource, Container targetContainer, IItemValueStore<T> targetItemValueStore)
			throws ServerFault;
}
