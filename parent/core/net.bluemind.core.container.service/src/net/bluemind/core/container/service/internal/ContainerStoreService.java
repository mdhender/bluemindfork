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

import static net.bluemind.core.container.service.internal.ReadOnlyMode.checkWritable;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.MoreObjects;
import com.google.common.base.Suppliers;
import com.google.common.collect.Lists;

import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.backup.continuous.api.IBackupStore;
import net.bluemind.core.backup.continuous.api.Providers;
import net.bluemind.core.caches.registry.CacheRegistry;
import net.bluemind.core.caches.registry.ICacheRegistration;
import net.bluemind.core.container.api.Count;
import net.bluemind.core.container.model.BaseContainerDescriptor;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.CountFastPath;
import net.bluemind.core.container.model.IdQuery;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemChangelog;
import net.bluemind.core.container.model.ItemFlag;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.ItemIdentifier;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.ItemVersion;
import net.bluemind.core.container.persistence.ChangelogStore;
import net.bluemind.core.container.persistence.ChangelogStore.LogEntry;
import net.bluemind.core.container.persistence.IItemValueStore;
import net.bluemind.core.container.persistence.IWeightProvider;
import net.bluemind.core.container.persistence.ItemStore;
import net.bluemind.core.container.service.ChangelogRenderers;
import net.bluemind.core.container.service.IContainerStoreService;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcAbstractStore;
import net.bluemind.core.jdbc.JdbcAbstractStore.SqlOperation;
import net.bluemind.directory.api.ReservedIds;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.system.api.SystemState;
import net.bluemind.system.state.StateContext;

public class ContainerStoreService<T> implements IContainerStoreService<T> {

	public <W> W doOrFail(SqlOperation<W> op) {
		return JdbcAbstractStore.doOrFail(op);
	}

	protected static final Logger logger = LoggerFactory.getLogger(ContainerStoreService.class);
	protected final Container container;
	protected final String containerCacheKey;
	protected ItemStore itemStore;
	protected IItemValueStore<T> itemValueStore;
	protected ChangelogStore changelogStore;
	private AclService aclService;
	private String origin;
	protected SecurityContext securityContext;
	protected boolean hasChangeLog = true;
	private final IItemFlagsProvider<T> flagsProvider;
	private final IWeightSeedProvider<T> weightSeedProvider;
	private final IWeightProvider weightProvider;
	private final Supplier<ContainerChangeEventProducer> containerChangeEventProducer;
	private final Supplier<IBackupStore<T>> backupStream;
	private final Supplier<Optional<ItemValueAuditLogService<T>>> logServiceSupplier;
	private final DataSource pool;

	public final ReservedIds.ConsumerHandler doNothingOnIdsReservation = callback -> callback.accept(null);

	private static final Cache<String, Long> lastEmptyChangeset = Caffeine.newBuilder()
			.expireAfterWrite(5, TimeUnit.MINUTES).recordStats().build();

	public static class EmptyChangesetReg implements ICacheRegistration {

		@Override
		public void registerCaches(CacheRegistry cr) {
			cr.register("known.empty.changesets", lastEmptyChangeset);
		}

	}

	public static interface IItemFlagsProvider<W> {
		Collection<ItemFlag> flags(W value);
	}

	public static interface IWeightSeedProvider<W> {

		long weightSeed(W value);

	}

	private static final Collection<ItemFlag> UNFLAGGED = EnumSet.noneOf(ItemFlag.class);

	public ContainerStoreService(DataSource pool, SecurityContext securityContext, Container container,
			IItemValueStore<T> itemValueStore, ItemValueAuditLogService<T> logService) {
		this(pool, securityContext, container, itemValueStore, v -> UNFLAGGED, v -> 0L, seed -> seed, logService);
	}

	public ContainerStoreService(DataSource pool, SecurityContext securityContext, Container container,
			IItemValueStore<T> itemValueStore) {
		this(pool, securityContext, container, itemValueStore, v -> UNFLAGGED, v -> 0L, seed -> seed,
				new ItemValueAuditLogService<>(securityContext, container.asDescriptor(null)));
	}

	public ContainerStoreService(DataSource pool, SecurityContext securityContext, Container container,
			IItemValueStore<T> itemValueStore, IItemFlagsProvider<T> fProv, IWeightSeedProvider<T> wsProv,
			IWeightProvider wProv) {
		this(pool, securityContext, container, itemValueStore, fProv, v -> 0L, seed -> seed,
				new ItemValueAuditLogService<>(securityContext, container.asDescriptor(null)));
	}

	public ContainerStoreService(DataSource pool, SecurityContext securityContext, Container container,
			IItemValueStore<T> itemValueStore, IItemFlagsProvider<T> fProv, IWeightSeedProvider<T> wsProv,
			IWeightProvider wProv, ItemValueAuditLogService<T> logService) {
		this.container = container;
		this.containerCacheKey = container.uid + "#" + container.id;
		this.securityContext = securityContext;
		this.origin = securityContext.getOrigin();
		this.itemStore = new ItemStore(pool, container, securityContext);
		this.changelogStore = new ChangelogStore(pool, container);
		this.itemValueStore = itemValueStore;
		this.aclService = new AclService(null, securityContext, pool, container);
		this.flagsProvider = fProv;
		this.weightSeedProvider = wsProv;
		this.weightProvider = wProv;
		this.logServiceSupplier = () -> {
			if (StateContext.getState().equals(SystemState.CORE_STATE_RUNNING)) {
				return Optional.of(logService);
			} else {
				return Optional.empty();
			}
		};

		BaseContainerDescriptor descriptor = BaseContainerDescriptor.create(container.uid, container.name,
				container.owner, container.type, container.domainUid, container.defaultContainer);
		descriptor.internalId = container.id;

		this.backupStream = Suppliers.memoize(() -> {
			IBackupStore<T> store = Providers.get().forContainer(descriptor);
			if (logger.isDebugEnabled()) {
				logger.debug("Using {} for {} backup.", store, container);
			}
			return store;
		});
		this.containerChangeEventProducer = Suppliers
				.memoize(() -> new ContainerChangeEventProducer(securityContext, VertxPlatform.eventBus(), container));

		this.pool = pool;
	}

	public ContainerStoreService<T> withoutChangelog() {
		hasChangeLog = false;
		return this;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(getClass()).add("items", itemValueStore).add("cont", container).toString();
	}

	private void assertChangeLog() {
		if (!hasChangeLog) {
			throw new ServerFault("no changelog for this container");
		}
	}

	public Count count(ItemFlagFilter filter) {
		Optional<CountFastPath> fastPath = filter.availableFastPath();
		if (fastPath.isPresent()) {
			return itemStore.fastpathCount(fastPath.get()).orElseGet(() -> {
				try {
					return Count.of(itemStore.count(filter));
				} catch (SQLException e) {
					throw ServerFault.sqlFault(e);
				}
			});
		}
		try {
			return Count.of(itemStore.count(filter));
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public ItemChangelog changelog(String itemUid, Long from, long to) {
		assertChangeLog();
		final Long since = null == from ? 0L : from;
		return ChangelogRenderers.render(securityContext,
				doOrFail(() -> changelogStore.itemChangelog(itemUid, since, to)));
	}

	protected void invalidateLastEmptyChangeset() {
		lastEmptyChangeset.invalidate(containerCacheKey);
	}

	private <W> ContainerChangeset<W> cacheIfUnchanged(long from, SqlOperation<ContainerChangeset<W>> op) {
		Long maybeSameAsSince = lastEmptyChangeset.getIfPresent(containerCacheKey);
		if (maybeSameAsSince != null && maybeSameAsSince.longValue() == from) {
			return ContainerChangeset.empty(from);
		}
		ContainerChangeset<W> cs = doOrFail(op);
		if (cs.version == from) {
			lastEmptyChangeset.put(containerCacheKey, from);
		}
		return cs;
	}

	public ContainerChangeset<String> changeset(Long from, long to) {
		assertChangeLog();
		final long since = null == from ? 0L : from;
		return cacheIfUnchanged(since, () -> changelogStore.changeset(weightProvider, since, to));
	}

	public ContainerChangeset<Long> changesetById(Long from, long to) {
		assertChangeLog();
		final long since = null == from ? 0L : from;
		return cacheIfUnchanged(since, () -> changelogStore.changesetById(weightProvider, since, to));
	}

	public ContainerChangeset<ItemIdentifier> fullChangesetById(Long from, long to) {
		assertChangeLog();
		final long since = null == from ? 0L : from;
		return cacheIfUnchanged(since, () -> changelogStore.fullChangesetById(weightProvider, since, to));
	}

	public ContainerChangeset<ItemVersion> changesetById(long from, ItemFlagFilter filter) {
		assertChangeLog();
		return doOrFail(() -> {
			// trying to cache those introduces invalidation complexity
			return changelogStore.changesetById(weightProvider, from, Long.MAX_VALUE, filter);
		});
	}

	@Override
	public ItemValue<T> get(String uid, Long version) {
		return doOrFail(() -> {
			Item item = itemStore.get(uid);
			if (version != null && item != null && item.version != version) {
				logger.warn("call get with version and version are different : expected {} actual {}", version,
						item.version);
			}

			return getItemValue(item);
		});

	}

	@Override
	public ItemValue<T> get(long id, Long version) {
		return doOrFail(() -> {
			Item item = itemStore.getById(id);
			if (version != null && item != null && item.version != version) {
				logger.warn("call get with version and version are different : expected {} actual {}", version,
						item.version);
			}

			return getItemValue(item);
		});

	}

	protected ItemValue<T> getItemValue(Item item) {
		if (item == null) {
			return null;
		}
		T value = getValue(item);
		if (value == null && !itemValueStore.toString().equals("UserSettingsStore")) {
			logger.warn("null value for existing item {} with store {}", item, itemValueStore);
		}
		ItemValue<T> ret = ItemValue.create(item, value);
		decorate(item, ret);
		return ret;
	}

	@Override
	public ItemValue<T> getByExtId(String extId) {
		return doOrFail(() -> {
			Item item = itemStore.getByExtId(extId);
			if (item == null) {
				return null;
			}

			return getItemValue(item);
		});
	}

	protected T getValue(Item item) {
		try {
			return itemValueStore.get(item);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public ItemVersion create(String uid, String displayName, T value) {
		return create(uid, null, displayName, value);
	}

	@Override
	public ItemVersion create(String uid, String extId, String displayName, T value) {
		return createWithId(uid, null, extId, displayName, value);
	}

	protected ItemVersion createWithId(String uid, Long internalId, String extId, String displayName, T value, // NOSONAR
			ChangelogStore changelogStore, ItemStore itemStore, IItemValueStore<T> itemValueStore) {
		Item item = new Item();
		item.uid = uid;
		item.externalId = extId;
		if (internalId != null) {
			item.id = internalId;
		}
		item.displayName = displayName;
		item.flags = flagsProvider.flags(value);
		return create(item, value, changelogStore, itemStore, itemValueStore, doNothingOnIdsReservation);
	}

	@Override
	public ItemVersion createWithId(String uid, Long internalId, String extId, String displayName, T value) {
		return createWithId(uid, internalId, extId, displayName, value, changelogStore, itemStore, itemValueStore);
	}

	@Override
	public ItemVersion create(Item item, T value) {
		return create(item, value, changelogStore, itemStore, itemValueStore, doNothingOnIdsReservation);
	}

	protected ItemVersion create(Item item, T value, ReservedIds.ConsumerHandler handler) {
		return create(item, value, changelogStore, itemStore, itemValueStore, handler);
	}

	private ItemVersion create(Item item, T value, ChangelogStore changelogStore, ItemStore itemStore,
			IItemValueStore<T> itemValueStore, ReservedIds.ConsumerHandler handler) {
		checkWritable();

		String uid = item.uid;
		Long internalId = item.id;
		return doOrFail(() -> {
			Item created;
			try {
				created = itemStore.create(item);
			} catch (SQLException e) {
				throw ServerFault.alreadyExists("entry[" + uid + " - " + internalId + "]@" + container.uid
						+ " already exists (" + e.getMessage() + ")");
			}
			if (created == null) {
				throw new ServerFault(
						"itemStore " + itemStore + " has **NOT** created item " + item + " can't continue");
			}

			createValue(created, value, itemValueStore);
			if (hasChangeLog) {
				changelogStore.itemCreated(LogEntry.create(created.version, created.uid, created.externalId,
						securityContext.getSubject(), origin, created.id, weightSeedProvider.weightSeed(value)));
			}
			lastEmptyChangeset.invalidate(containerCacheKey);
			if (hasChangeLog) {
				containerChangeEventProducer.get().produceEvent();
			}

			ItemValue<T> iv = ItemValue.create(created, value);
			beforeCreationInBackupStore(iv);

			handler.acceptConsumer(reservedIds -> backupStream.get().store(iv, reservedIds));
			logServiceSupplier.get().ifPresent(logService -> {
				logService.logCreate(iv);
			});
			return created.itemVersion();
		});
	}

	protected void beforeCreationInBackupStore(@SuppressWarnings("unused") ItemValue<T> itemValue) {
		// This methode can be override in child class to perform an operation before
		// backuping a creation
	}

	@Override
	public void attach(String uid, String displayName, T value) {
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
				containerChangeEventProducer.get().produceEvent();
			}
			createValue(item, value);
			lastEmptyChangeset.invalidate(containerCacheKey);
			return null;
		});
	}

	private void createValue(Item item, T value) throws SQLException {
		createValue(item, value, itemValueStore);
	}

	protected void createValue(Item item, T value, IItemValueStore<T> itemValueStore) throws SQLException {
		itemValueStore.create(item, value);
	}

	@Override
	public ItemVersion update(String uid, String displayName, T value) {
		Item item = new Item();
		item.uid = uid;
		return update(item, displayName, value);
	}

	@Override
	public ItemVersion update(Item item, String displayName, T value) {
		return update(item, displayName, value, doNothingOnIdsReservation);
	}

	protected ItemVersion update(Item item, String displayName, T value, ReservedIds.ConsumerHandler handler) {
		checkWritable();

		return doOrFail(() -> {

			String dnToApply = displayName;
			if (dnToApply == null) {
				// try to preserve the existing display name
				Item existing = itemStore.getForUpdate(item.uid);
				if (existing == null) {
					throw ServerFault
							.notFound("entry[" + item.uid + "]@" + container.uid + " not found in pool " + pool);
				}

				dnToApply = existing.displayName;
			}
			Item updated = itemStore.update(item, dnToApply, flagsProvider.flags(value));
			if (updated == null) {
				throw ServerFault.notFound("entry[uid: " + item.uid + " / id:" + item.id + "]@" + container.uid
						+ " not found, dn: " + dnToApply + ", in pool " + pool);
			}

			if (hasChangeLog) {
				changelogStore.itemUpdated(LogEntry.create(updated.version, updated.uid, updated.externalId,
						securityContext.getSubject(), origin, updated.id, weightSeedProvider.weightSeed(value)));
			}

			T oldValue = doOrFail(() -> itemValueStore.get(updated));
			updateValue(updated, value);
			lastEmptyChangeset.invalidate(containerCacheKey);
			if (hasChangeLog) {
				containerChangeEventProducer.get().produceEvent();
			}

			handler.acceptConsumer(
					reservedIds -> backupStream.get().store(ItemValue.create(updated, value), reservedIds));
			logServiceSupplier.get().ifPresent(logService -> {
				ItemValue<T> itemValue = ItemValue.create(updated, value);
				if (updated.flags.contains(ItemFlag.Deleted)) {
					logService.logDelete(itemValue);
				} else {
					logService.logUpdate(itemValue, oldValue);
				}
			});
			return updated.itemVersion();
		});
	}

	@Override
	public ItemVersion update(long itemId, String displayName, T value) {
		checkWritable();

		return doOrFail(() -> {

			String dnToApply = displayName;
			if (dnToApply == null) {
				// try to preserve the existing display name
				Item existing = itemStore.getForUpdate(itemId);
				if (existing == null) {
					throw ServerFault
							.notFound("entry[id: " + itemId + "]@" + container.uid + " not found in pool " + pool);
				}

				dnToApply = existing.displayName;
			}

			Item item = itemStore.update(itemId, dnToApply, flagsProvider.flags(value));
			if (item == null) {
				throw ServerFault.notFound("entry[id: " + itemId + "]@" + container.uid + " not found in pool " + pool);
			}
			if (hasChangeLog) {
				changelogStore.itemUpdated(LogEntry.create(item.version, item.uid, item.externalId,
						securityContext.getSubject(), origin, item.id, weightSeedProvider.weightSeed(value)));
				containerChangeEventProducer.get().produceEvent();
			}
			T oldValue = doOrFail(() -> itemValueStore.get(item));
			updateValue(item, value);
			logServiceSupplier.get().ifPresent(logService -> {
				ItemValue<T> itemValue = ItemValue.create(item, value);
				if (item.flags.contains(ItemFlag.Deleted)) {
					logService.logDelete(itemValue);
				} else {
					logService.logUpdate(itemValue, oldValue);
				}
			});
			lastEmptyChangeset.invalidate(containerCacheKey);

			ItemValue<T> iv = ItemValue.create(item, value);
			backupStream.get().store(iv);

			return item.itemVersion();
		});
	}

	protected void preUpdateValue(Item newItem, T newValue, Supplier<T> oldValue) throws SQLException {
		// override if necessary
	}

	protected void updateValue(Item item, T value) throws SQLException {
		preUpdateValue(item, value, () -> doOrFail(() -> itemValueStore.get(item)));
		itemValueStore.update(item, value);
	}

	@Override
	public ItemVersion delete(String uid) {
		checkWritable();

		return doOrFail(() -> {
			Item item = itemStore.getForUpdate(uid);
			if (item == null) {
				return null;
			}
			item = itemStore.touch(uid);
			ItemValue<T> itemValue = getItemValue(item);
			deleteValue(item);
			if (hasChangeLog) {
				changelogStore.itemDeleted(LogEntry.create(item.version, item.uid, item.externalId,
						securityContext.getSubject(), origin, item.id, 0L));
				containerChangeEventProducer.get().produceEvent();
			}
			itemStore.delete(item);
			lastEmptyChangeset.invalidate(containerCacheKey);
			ContainerDescriptor cd = ContainerDescriptor.create(container.uid, container.name, container.owner,
					container.type, container.domainUid, false);
			cd.internalId = container.id;
			backupStream.get().delete(itemValue);
			logServiceSupplier.get().ifPresent(logService -> {
				logService.logDelete(itemValue);
			});
			return item.itemVersion();
		});
	}

	@Override
	public ItemVersion delete(long id) {
		checkWritable();

		return doOrFail(() -> {
			Item item = itemStore.getForUpdate(id);
			if (item == null) {
				return null;
			}
			item = itemStore.touch(item.uid);
			ItemValue<T> itemValue = getItemValue(item);
			deleteValue(item);
			if (hasChangeLog) {
				changelogStore.itemDeleted(LogEntry.create(item.version, item.uid, item.externalId,
						securityContext.getSubject(), origin, item.id, 0L));
				containerChangeEventProducer.get().produceEvent();
			}
			itemStore.delete(item);
			lastEmptyChangeset.invalidate(containerCacheKey);

			ContainerDescriptor cd = ContainerDescriptor.create(container.uid, container.name, container.owner,
					container.type, container.domainUid, false);
			cd.internalId = container.id;
			backupStream.get().delete(itemValue);

			return item.itemVersion();
		});
	}

	@Override
	public void detach(String uid) {

		doOrFail(() -> {
			Item item = itemStore.getForUpdate(uid);
			if (item == null) {
				return null;
			}
			item = itemStore.touch(uid);
			deleteValue(item);
			lastEmptyChangeset.invalidate(containerCacheKey);
			if (hasChangeLog) {
				changelogStore.itemUpdated(LogEntry.create(item.version, item.uid, item.externalId,
						securityContext.getSubject(), origin, item.id, 0L));
				containerChangeEventProducer.get().produceEvent();
			}
			return null;
		});
	}

	protected void deleteValue(Item item) throws SQLException {
		itemValueStore.delete(item);
	}

	@Override
	public void deleteAll() {
		checkWritable();

		doOrFail(() -> {
			// delete values
			deleteValues();
			// delete container
			if (hasChangeLog) {
				changelogStore.allItemsDeleted(securityContext.getSubject(), origin);
				containerChangeEventProducer.get().produceEvent();
			}
			// delete items
			itemStore.deleteAll();
			lastEmptyChangeset.invalidate(containerCacheKey);
			return null;
		});
	}

	@Override
	public void prepareContainerDelete() {
		checkWritable();

		doOrFail(() -> {
			// delete acl
			aclService.deleteAll();
			// delete values
			deleteValues();
			// delete changelog
			if (hasChangeLog) {
				changelogStore.deleteLog();
			}
			// delete items
			itemStore.deleteAll();
			lastEmptyChangeset.invalidate(containerCacheKey);
			return null;
		});
	}

	protected void deleteValues() {
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

	public List<ItemValue<T>> getItemsValue(List<Item> items) {

		List<ItemValue<T>> ret = new ArrayList<>(items.size());

		List<T> values = null;
		try {
			values = itemValueStore.getMultiple(items);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
		List<Item> nonNullValues = new ArrayList<>(items.size());

		if (values.size() == items.size()) {
			Iterator<Item> itItems = items.iterator();
			Iterator<T> itValues = values.iterator();

			while (itItems.hasNext()) {
				Item item = itItems.next();
				T value = itValues.next();
				if (value != null) {
					ret.add(ItemValue.create(item, value));
					nonNullValues.add(item);
				}
			}

			decorate(nonNullValues, ret);
			return ret;
		} else {
			logger.warn("Mismatch in value and item count on container {}", container.uid);
			return getItemsValueByIndividualLookup(items);
		}
	}

	private List<ItemValue<T>> getItemsValueByIndividualLookup(List<Item> items) {
		List<ItemValue<T>> ret = new ArrayList<>(items.size());

		for (Item item : items) {
			try {
				T value = itemValueStore.get(item);
				if (value != null) {
					ItemValue<T> itemValue = ItemValue.create(item, value);
					ret.add(itemValue);
					decorate(item, itemValue);
				}
			} catch (SQLException e) {
				throw ServerFault.sqlFault(e);
			}
		}
		return ret;
	}

	protected void decorate(List<Item> items, List<ItemValue<T>> values) {
		Iterator<ItemValue<T>> it = values.iterator();
		for (Item item : items) {
			decorate(item, it.next());
		}
	}

	@SuppressWarnings("unused")
	protected void decorate(Item item, ItemValue<T> value) {
		// OK
	}

	public List<ItemValue<T>> getMultiple(List<String> uids) {
		if (uids == null || uids.isEmpty()) {
			return Collections.emptyList();
		}
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

	public List<ItemValue<T>> getMultipleById(List<Long> ids) {
		if (ids == null || ids.isEmpty()) {
			return Collections.emptyList();
		}
		return doOrFail(() -> {
			List<Item> items = null;

			try {
				items = itemStore.getMultipleById(ids);
			} catch (SQLException e) {
				throw ServerFault.sqlFault(e);
			}

			return getItemsValue(items);
		});
	}

	public List<ItemValue<T>> all() {
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
	public ItemVersion touch(String uid) {
		checkWritable();

		return doOrFail(() -> {
			Item item = itemStore.touch(uid);

			if (item == null) {
				throw ServerFault.notFound("entry[" + uid + "]@" + container.uid + " not found in pool " + pool);
			}
			lastEmptyChangeset.invalidate(containerCacheKey);

			if (hasChangeLog) {
				T value = getValue(item);
				changelogStore.itemUpdated(LogEntry.create(item.version, item.uid, item.externalId,
						securityContext.getSubject(), origin, item.id, weightSeedProvider.weightSeed(value)));

				ItemValue<T> iv = ItemValue.create(item, value);
				backupStream.get().store(iv);

				containerChangeEventProducer.get().produceEvent();
			}
			return item.itemVersion();
		});
	}

	@Override
	public List<String> allUids() {

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
	public ListResult<Long> allIds(IdQuery query) {
		if (query.offset < 0 || query.limit < 0) {
			throw new ServerFault("negative offset or limit");
		}
		try {
			long len = query.filter == null ? itemStore.getItemCount() : itemStore.count(query.filter);
			List<Item> items = query.filter == null ? itemStore.all()
					: itemStore.filtered(query.filter, query.offset, query.limit);

			// check after because it can change...
			long serverVersion = getVersion();
			if (query.knownContainerVersion != serverVersion) {
				throw new ServerFault("stale client version, server is at " + serverVersion,
						ErrorCode.VERSION_HAS_CHANGED);
			}
			return ListResult.create(items.stream().map(i -> i.id).collect(Collectors.toList()), len);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}

	}

	public long getVersion() {
		try {
			return itemStore.getVersion();
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public long setExtId(String uid, String extId) {
		return doOrFail(() -> {
			Item item = itemStore.setExtId(uid, extId);

			if (item == null) {
				throw ServerFault.notFound("entry[" + uid + "]@" + container.uid + " not found in pool " + pool);
			}
			changelogStore.itemUpdated(LogEntry.create(item.version, item.uid, item.externalId,
					securityContext.getSubject(), origin, item.id, 0L));
			return item.version;
		});
	}

	@Override
	public void xfer(javax.sql.DataSource targetDataSource, Container targetContainer,
			IItemValueStore<T> targetItemValueStore) {
		logger.info("Starting xfer to {} with {} on db {}", targetContainer, itemStore, targetDataSource);
		try {
			ItemStore targetItemStore = new ItemStore(targetDataSource, targetContainer, securityContext);
			ChangelogStore targetChangelogStore = new ChangelogStore(targetDataSource, targetContainer);
			long transferred = 0;
			List<List<Long>> uidPartitions = Lists.partition(itemStore.allItemIds(), 4096);
			for (List<Long> uids : uidPartitions) {
				for (Item oldItem : itemStore.getMultipleById(uids)) {
					createWithId(oldItem.uid, null, oldItem.externalId, oldItem.displayName, getValue(oldItem),
							targetChangelogStore, targetItemStore, targetItemValueStore);
					transferred++;
				}
			}
			logger.info("xfer container uid={}: transferred {} items", targetContainer.uid, transferred);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		} catch (Exception e) {
			throw new ServerFault(e);
		}

		doOrFail(() -> {
			prepareContainerDelete();
			return null;
		});
	}

}