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
package net.bluemind.core.container.service.internal;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.Count;
import net.bluemind.core.container.model.ChangeLogEntry;
import net.bluemind.core.container.model.ChangeLogEntry.Type;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerChangelog;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemChangelog;
import net.bluemind.core.container.model.ItemFlag;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.ItemVersion;
import net.bluemind.core.container.persistance.AclStore;
import net.bluemind.core.container.persistance.ChangelogStore;
import net.bluemind.core.container.persistance.ChangelogStore.LogEntry;
import net.bluemind.core.container.persistance.IItemValueStore;
import net.bluemind.core.container.persistance.IWeightProvider;
import net.bluemind.core.container.persistance.ItemStore;
import net.bluemind.core.container.service.IContainerStoreService;
import net.bluemind.core.container.service.ItemUpdate;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcAbstractStore;
import net.bluemind.core.jdbc.JdbcAbstractStore.SqlOperation;
import net.bluemind.lib.vertx.VertxPlatform;

public class ContainerStoreService<T> implements IContainerStoreService<T> {

	public <Res> Res doOrFail(SqlOperation<Res> op) throws ServerFault {
		return JdbcAbstractStore.doOrFail(op);
	}

	protected static final Logger logger = LoggerFactory.getLogger(ContainerStoreService.class);
	protected Container container;
	protected ItemStore itemStore;
	protected IItemValueStore<T> itemValueStore;
	protected ChangelogStore changelogStore;
	private AclStore aclStore;
	private String origin;
	protected SecurityContext securityContext;
	protected boolean hasChangeLog = true;
	private final IItemFlagsProvider<T> flagsProvider;
	private final IWeightSeedProvider<T> weightSeedProvider;
	private final IWeightProvider weightProvider;
	private ContainerChangeEventProducer containerChangeEventProducer;

	public static interface IItemFlagsProvider<W> {
		Collection<ItemFlag> flags(W value);
	}

	public static interface IWeightSeedProvider<W> {

		long weightSeed(W value);

	}

	private static final Collection<ItemFlag> UNFLAGGED = EnumSet.noneOf(ItemFlag.class);

	public ContainerStoreService(DataSource pool, SecurityContext securityContext, Container container, String itemType,
			IItemValueStore<T> itemValueStore, IItemFlagsProvider<T> fProv, IWeightSeedProvider<T> wsProv,
			IWeightProvider wProv) {
		this.container = container;
		this.securityContext = securityContext;
		this.origin = securityContext.getOrigin();
		this.itemStore = new ItemStore(pool, container, securityContext);
		this.changelogStore = new ChangelogStore(pool, container);
		this.itemValueStore = itemValueStore;
		this.aclStore = new AclStore(pool);
		this.flagsProvider = fProv;
		this.weightSeedProvider = wsProv;
		this.weightProvider = wProv;
		this.containerChangeEventProducer = new ContainerChangeEventProducer(securityContext, VertxPlatform.eventBus(),
				container);
	}

	public ContainerStoreService(DataSource pool, SecurityContext securityContext, Container container, String itemType,
			IItemValueStore<T> itemValueStore) {
		this(pool, securityContext, container, itemType, itemValueStore, (v) -> UNFLAGGED, (v) -> 0L, seed -> seed);
	}

	@Override
	public ContainerChangelog changelog(Long from, long to) throws ServerFault {
		if (!hasChangeLog) {
			throw new ServerFault("no changelog for this container");
		}
		final Long since = null == from ? 0L : from;
		return doOrFail(() -> {
			return changelogStore.changelog(since, to);
		});
	}

	public Count count(ItemFlagFilter filter) {
		try {
			return Count.of(itemStore.count(filter));
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public ItemChangelog changelog(String itemUid, Long from, long to) throws ServerFault {
		if (!hasChangeLog) {
			throw new ServerFault("no changelog for this container");
		}
		final Long since = null == from ? 0L : from;
		return doOrFail(() -> {
			return changelogStore.itemChangelog(itemUid, since, to);
		});
	}

	public ContainerChangeset<String> changeset(Long from, long to) throws ServerFault {
		if (!hasChangeLog) {
			throw new ServerFault("no changelog for this container");
		}
		final long since = null == from ? 0L : from;
		return doOrFail(() -> {
			try {
				return changelogStore.changeset(weightProvider, since, to);
			} catch (SQLException e) {
				throw ServerFault.sqlFault(e);
			}
		});
	}

	public ContainerChangeset<Long> changesetById(Long from, long to) throws ServerFault {
		if (!hasChangeLog) {
			throw new ServerFault("no changelog for this container");
		}
		final long since = null == from ? 0L : from;
		return doOrFail(() -> {
			try {
				return changelogStore.changesetById(weightProvider, since, to);
			} catch (SQLException e) {
				throw ServerFault.sqlFault(e);
			}
		});
	}

	public ContainerChangeset<ItemVersion> changesetById(long from, ItemFlagFilter filter) throws ServerFault {
		if (!hasChangeLog) {
			throw new ServerFault("no changelog for this container");
		}
		return doOrFail(() -> {
			try {
				return changelogStore.changesetById(weightProvider, from, Long.MAX_VALUE, filter);
			} catch (SQLException e) {
				throw ServerFault.sqlFault(e);
			}
		});
	}

	@Override
	public ItemValue<T> get(String uid, Long version) throws ServerFault {
		return doOrFail(() -> {
			Item item = itemStore.get(uid);
			if (version != null && item != null && item.version != version) {
				logger.warn("call get with version and version are different : expected {} actual {}", version,
						item.version);
				// FIXME throw exception WrongVersion
				// FIXME throw ServerFault
			}

			return getItemValue(item);
		});

	}

	@Override
	public ItemValue<T> get(long id, Long version) throws ServerFault {
		return doOrFail(() -> {
			Item item = itemStore.getById(id);
			if (version != null && item != null && item.version != version) {
				logger.warn("call get with version and version are different : expected {} actual {}", version,
						item.version);
				// FIXME throw exception WrongVersion
				// FIXME throw ServerFault
			}

			return getItemValue(item);
		});

	}

	protected ItemValue<T> getItemValue(Item item) throws ServerFault {
		if (item == null) {
			return null;
		}
		T value = getValue(item);
		ItemValue<T> ret = ItemValue.create(item, value);
		decorate(item, ret);
		return ret;
	}

	@Override
	public ItemValue<T> getByExtId(String extId) throws ServerFault {
		return doOrFail(() -> {
			Item item = itemStore.getByExtId(extId);
			if (item == null) {
				return null;
			}

			return getItemValue(item);
		});
	}

	protected T getValue(Item item) throws ServerFault {
		try {
			return itemValueStore.get(item);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public ItemVersion create(String uid, String displayName, T value) throws ServerFault {
		return create(uid, null, displayName, value);
	}

	@Override
	public ItemVersion create(String uid, String extId, String displayName, T value) throws ServerFault {
		return createWithId(uid, null, extId, displayName, value);
	}

	@Override
	public ItemVersion createWithId(String uid, Long internalId, String extId, String displayName, T value)
			throws ServerFault {
		return doOrFail(() -> {

			Item item = null;
			try {
				item = new Item();
				item.uid = uid;
				item.externalId = extId;
				if (internalId != null) {
					item.id = internalId;
				}
				item.displayName = displayName;
				item.flags = flagsProvider.flags(value);
				item = itemStore.create(item);
			} catch (SQLException e) {
				throw ServerFault
						.alreadyExists("entry[" + uid + " - " + internalId + "]@" + container.uid + " already exists");
			}
			if (hasChangeLog) {
				changelogStore.itemCreated(LogEntry.create(item.version, item.uid, item.externalId,
						securityContext.getSubject(), origin, item.id, weightSeedProvider.weightSeed(value)));
				this.containerChangeEventProducer.produceEvent();
			}
			createValue(item, value);
			return ItemUpdate.of(item);
		});
	}

	@Override
	public void attach(String uid, String displayName, T value) throws ServerFault {
		doOrFail(() -> {

			Item item = itemStore.getForUpdate(uid);
			if (item == null) {
				item = new Item();
				item.uid = uid;
				item.displayName = displayName;
				item = itemStore.create(item);
			} else {
				item = itemStore.touch(uid);
			}

			if (hasChangeLog) {
				changelogStore.itemUpdated(LogEntry.create(item.version, item.uid, item.externalId,
						securityContext.getSubject(), origin, item.id, weightSeedProvider.weightSeed(value)));
				this.containerChangeEventProducer.produceEvent();
			}
			createValue(item, value);
			return null;
		});
	}

	protected void createValue(Item item, T value) throws ServerFault, SQLException {
		itemValueStore.create(item, value);
	}

	@Override
	public ItemVersion update(String uid, String displayName, T value) throws ServerFault {
		return doOrFail(() -> {

			Item item = itemStore.getForUpdate(uid);
			if (item == null) {
				throw ServerFault.notFound("entry[" + uid + "]@" + container.uid + " not found");
			}

			if (displayName != null) {
				item.displayName = displayName;
			}
			item = itemStore.update(uid, item.displayName, flagsProvider.flags(value));
			if (hasChangeLog) {
				changelogStore.itemUpdated(LogEntry.create(item.version, item.uid, item.externalId,
						securityContext.getSubject(), origin, item.id, weightSeedProvider.weightSeed(value)));
				this.containerChangeEventProducer.produceEvent();
			}
			updateValue(item, value);
			return ItemUpdate.of(item);
		});
	}

	@Override
	public ItemVersion update(long itemId, String displayName, T value) throws ServerFault {
		return doOrFail(() -> {

			Item item = itemStore.getForUpdate(itemId);
			if (item == null) {
				throw ServerFault.notFound("entry[" + itemId + "]@" + container.uid + " not found");
			}

			if (displayName != null) {
				item.displayName = displayName;
			}

			item = itemStore.update(item.uid, item.displayName, flagsProvider.flags(value));
			if (hasChangeLog) {
				changelogStore.itemUpdated(LogEntry.create(item.version, item.uid, item.externalId,
						securityContext.getSubject(), origin, item.id, weightSeedProvider.weightSeed(value)));
				this.containerChangeEventProducer.produceEvent();
			}
			updateValue(item, value);
			return ItemUpdate.of(item);
		});
	}

	protected void updateValue(Item item, T value) throws ServerFault, SQLException {
		itemValueStore.update(item, value);
	}

	@Override
	public ItemVersion delete(String uid) throws ServerFault {
		return doOrFail(() -> {
			Item item = itemStore.getForUpdate(uid);
			if (item == null) {
				return null;
			}
			item = itemStore.touch(uid);
			deleteValue(item);
			if (hasChangeLog) {
				changelogStore.itemDeleted(LogEntry.create(item.version, item.uid, item.externalId,
						securityContext.getSubject(), origin, item.id, 0L));
				this.containerChangeEventProducer.produceEvent();
			}
			itemStore.delete(item);
			return ItemUpdate.of(item);
		});
	}

	@Override
	public ItemVersion delete(long id) throws ServerFault {
		return doOrFail(() -> {
			Item item = itemStore.getForUpdate(id);
			if (item == null) {
				return null;
			}
			item = itemStore.touch(item.uid);
			deleteValue(item);
			if (hasChangeLog) {
				changelogStore.itemDeleted(LogEntry.create(item.version, item.uid, item.externalId,
						securityContext.getSubject(), origin, item.id, 0L));
				this.containerChangeEventProducer.produceEvent();
			}
			itemStore.delete(item);
			return ItemUpdate.of(item);
		});
	}

	@Override
	public void detach(String uid) throws ServerFault {
		doOrFail(() -> {
			Item item = itemStore.getForUpdate(uid);
			if (item == null) {
				return null;
				// throw ServerFault.notFound("entry[" + uid + "]@"
				// + container.uid + " not found");
			}
			item = itemStore.touch(uid);
			deleteValue(item);
			if (hasChangeLog) {
				changelogStore.itemUpdated(LogEntry.create(item.version, item.uid, item.externalId,
						securityContext.getSubject(), origin, item.id, 0L));
				this.containerChangeEventProducer.produceEvent();
			}
			return null;
		});
	}

	protected void deleteValue(Item item) throws ServerFault, SQLException {
		itemValueStore.delete(item);
	}

	@Override
	public void deleteAll() throws ServerFault {
		doOrFail(() -> {
			// delete values
			deleteValues();
			// delete container
			if (hasChangeLog) {
				changelogStore.allItemsDeleted(securityContext.getSubject(), origin);
				this.containerChangeEventProducer.produceEvent();
			}
			// delete items
			itemStore.deleteAll();
			return null;
		});
	}

	@Override
	public void prepareContainerDelete() throws ServerFault {
		doOrFail(() -> {
			// delete acl
			aclStore.deleteAll(container);
			// delete values
			deleteValues();
			// delete changelog
			changelogStore.deleteLog();
			// delete items
			itemStore.deleteAll();
			return null;
		});
	}

	protected void deleteValues() throws ServerFault {
		try {
			itemValueStore.deleteAll();
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	public ItemStore getItemStore() {
		return itemStore;
	}

	public IItemValueStore<T> getItemValueStore() {
		return itemValueStore;
	}

	public List<ItemValue<T>> getItemsValue(List<Item> items) throws ServerFault {

		List<ItemValue<T>> ret = new ArrayList<>(items.size());

		List<T> values = null;
		try {
			values = itemValueStore.getMultiple(items);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}

		Iterator<Item> itItems = items.iterator();
		Iterator<T> itValues = values.iterator();

		for (; itItems.hasNext();) {
			Item item = itItems.next();
			T value = itValues.next();
			ret.add(ItemValue.create(item, value));
		}

		decorate(items, ret);
		return ret;
	}

	protected void decorate(List<Item> items, List<ItemValue<T>> values) throws ServerFault {
		Iterator<ItemValue<T>> it = values.iterator();
		for (Item item : items) {
			decorate(item, it.next());
		}
	}

	protected void decorate(Item item, ItemValue<T> value) throws ServerFault {
		logger.debug("decorate {} {}", item, value);
	}

	public List<ItemValue<T>> getMultiple(List<String> uids) throws ServerFault {
		return doOrFail(() -> {
			List<Item> items = null;

			try {
				items = itemStore.getMultiple(uids);
			} catch (SQLException e) {
				throw ServerFault.sqlFault(e);
			}

			return getItemsValue(items);
		});
	}

	public List<ItemValue<T>> getMultipleById(List<Long> uids) throws ServerFault {
		return doOrFail(() -> {
			List<Item> items = null;

			try {
				items = itemStore.getMultipleById(uids);
			} catch (SQLException e) {
				throw ServerFault.sqlFault(e);
			}

			return getItemsValue(items);
		});
	}

	public List<ItemValue<T>> all() throws ServerFault {
		return doOrFail(() -> {
			List<Item> items = null;

			try {
				items = itemStore.all();
			} catch (SQLException e) {
				throw ServerFault.sqlFault(e);
			}

			return getItemsValue(items);
		});
	}

	@Override
	public void touch(String uid) throws ServerFault {
		doOrFail(() -> {
			Item item = itemStore.touch(uid);

			if (item == null) {
				throw ServerFault.notFound("entry[" + uid + "]@" + container.uid + " not found");
			}

			if (hasChangeLog) {
				T value = getValue(item);
				changelogStore.itemUpdated(LogEntry.create(item.version, item.uid, item.externalId,
						securityContext.getSubject(), origin, item.id, weightSeedProvider.weightSeed(value)));
				this.containerChangeEventProducer.produceEvent();
			}
			return null;
		});
	}

	@Override
	public List<String> allUids() throws ServerFault {

		try {
			List<Item> items = itemStore.all();
			List<String> ret = new ArrayList<>(items.size());
			for (Item i : items) {
				ret.add(i.uid);
			}
			return ret;
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}

	}

	@Override
	public List<String> allUidsOrderedByDisplayname() throws ServerFault {

		try {
			return itemStore.allItemUidsOrderedByDisplayname();
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}

	}

	public long getVersion() throws ServerFault {
		try {
			return itemStore.getVersion();
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public long setExtId(String uid, String extId) throws ServerFault {
		return doOrFail(() -> {
			Item item = itemStore.setExtId(uid, extId);

			if (item == null) {
				throw ServerFault.notFound("entry[" + uid + "]@" + container.uid + " not found");
			}
			changelogStore.itemUpdated(LogEntry.create(item.version, item.uid, item.externalId,
					securityContext.getSubject(), origin, item.id, 0L));
			return item.version;
		});
	}

	@Override
	public void xfer(javax.sql.DataSource targetDataSource, Container targetContainer,
			IItemValueStore<T> targetItemValueStore) throws ServerFault {
		logger.info("Starting xfer to {} with {} on db {}", targetContainer, itemStore, targetDataSource);
		try {
			List<String> uids = itemStore.allItemUids();
			if (uids.isEmpty()) {
				logger.info("xfer {}: no data", targetContainer.uid);
				return;
			}

			ContainerChangelog cc = changelogStore.changelog(0, Long.MAX_VALUE);

			ItemStore is = new ItemStore(targetDataSource, targetContainer, securityContext);
			ChangelogStore cs = new ChangelogStore(targetDataSource, targetContainer);

			for (ChangeLogEntry entry : cc.entries) {
				if (entry.type == Type.Created && uids.contains(entry.itemUid)) {
					ItemValue<T> itemValue = get(entry.itemUid, null);
					Item item = new Item();
					item.uid = itemValue.uid;
					item.externalId = itemValue.externalId;
					item.displayName = itemValue.displayName;
					item.flags = flagsProvider.flags(itemValue.value);
					item.version = itemValue.version;
					item = is.create(item);

					targetItemValueStore.create(item, itemValue.value);

				} else {
					is.touch(entry.itemUid);
				}
			}
			logger.info("xfer {}: {} items", targetContainer.uid, cc.entries.size());

			cs.insertLog(cc.entries);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		} catch (Exception e) {
			throw new ServerFault(e);
		}

		doOrFail(() -> {
			deleteAll();
			changelogStore.deleteLog();
			return null;
		});

	}

}
