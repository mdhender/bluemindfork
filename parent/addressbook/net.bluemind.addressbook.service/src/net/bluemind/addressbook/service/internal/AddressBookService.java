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
package net.bluemind.addressbook.service.internal;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.elasticsearch.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import net.bluemind.addressbook.api.IAddressBook;
import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCardChanges;
import net.bluemind.addressbook.api.VCardChanges.ItemAdd;
import net.bluemind.addressbook.api.VCardInfo;
import net.bluemind.addressbook.api.VCardQuery;
import net.bluemind.addressbook.api.VCardQuery.OrderBy;
import net.bluemind.addressbook.persistence.VCardIndexStore;
import net.bluemind.addressbook.persistence.VCardStore;
import net.bluemind.addressbook.service.IInCoreAddressBook;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.api.fault.ValidationException;
import net.bluemind.core.container.api.Ack;
import net.bluemind.core.container.api.Count;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerChangelog;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.ContainerSyncStatus;
import net.bluemind.core.container.model.ContainerUpdatesResult;
import net.bluemind.core.container.model.ItemChangelog;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.ItemVersion;
import net.bluemind.core.container.model.SortDescriptor;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.ContainerSyncStore;
import net.bluemind.core.container.persistence.DataSourceRouter;
import net.bluemind.core.container.service.ChangeLogUtil;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sanitizer.Sanitizer;
import net.bluemind.core.utils.DependencyResolver;
import net.bluemind.core.utils.ImageUtils;
import net.bluemind.core.validator.Validator;
import net.bluemind.lib.vertx.VertxPlatform;

public class AddressBookService implements IInCoreAddressBook {
	private static final Logger logger = LoggerFactory.getLogger(AddressBookService.class);

	private VCardContainerStoreService storeService;
	private SecurityContext securityContext;
	private AddressBookEventProducer eventProducer;
	private Sanitizer extSanitizer;
	private Validator extValidator;

	final Container container;

	private final BmContext context;

	private final VCardIndexStore indexStore;

	private final RBACManager rbacManager;

	private final VCardStore vcardStore;

	public AddressBookService(DataSource dataSource, Client esearchClient, Container container, BmContext context) {
		this.context = context;
		this.securityContext = context.getSecurityContext();
		this.container = container;

		this.vcardStore = new VCardStore(dataSource, container);
		this.eventProducer = new AddressBookEventProducer(container, securityContext, VertxPlatform.eventBus());
		indexStore = new VCardIndexStore(esearchClient, container);
		this.storeService = new VCardContainerStoreService(context, dataSource, securityContext, container, vcardStore,
				indexStore);

		extSanitizer = new Sanitizer(context);
		extValidator = new Validator(context);

		rbacManager = RBACManager.forContext(context).forContainer(container);
	}

	@Override
	public List<String> allUids() throws ServerFault {
		rbacManager.check(Verb.Read.name());
		return storeService.allUids();
	}

	@Override
	public void create(String uid, VCard card) throws ServerFault {
		rbacManager.check(Verb.Write.name());
		// ext point sanitizer
		doCreate(uid, null, card, null);
		eventProducer.vcardCreated(uid);
		eventProducer.changed();
	}

	@Override
	public Ack createById(long id, VCard card) throws ServerFault {
		rbacManager.check(Verb.Write.name());

		String uid = "vcard-by-id:" + id;
		long version = doCreate(uid, id, card, null);
		eventProducer.vcardCreated(uid);
		eventProducer.changed();
		logger.info("createdById {} ({}) => v{}", id, uid, version);
		return Ack.create(version);
	}

	@SuppressWarnings("serial")
	private long doCreate(String uid, Long internalId, VCard card, byte[] photo) throws ServerFault {
		// ext point sanitizer
		extSanitizer.create(card, new HashMap<String, String>() {
			{
				put("containerUid", container.uid);
			}
		});

		extValidator.create(card, ImmutableMap.of("containerUid", container.uid));

		ItemVersion version = storeService.createWithId(uid, internalId, null, getDisplayName(card), card);
		if (photo != null) {
			if (photo.length == 0) {
				storeService.deletePhoto(uid);
				storeService.deleteIcon(uid);
			} else {
				storeService.setPhoto(uid, photo);
				storeService.setIcon(uid, ImageUtils.resize(photo, 22, 22));
			}
		}
		return version.version;
	}

	private boolean doCreateOrUpdate(String uid, VCard value, byte[] photo) throws ServerFault {

		try {
			doCreate(uid, null, value, photo);
			return false;
		} catch (ServerFault sf) {
			if (sf.getCode() == ErrorCode.ALREADY_EXISTS) {
				logger.warn("vcard uid {} was sent as created but already exists. We update it", uid);
				return doUpdate(uid, null, value, photo).dnChanged;
			} else {
				throw sf;
			}
		}

	}

	@Override
	public void update(String uid, VCard card) throws ServerFault {
		rbacManager.check(Verb.Write.name());
		boolean importantChanges = doUpdate(uid, null, card, null).dnChanged;

		if (importantChanges) {
			refreshGroupsFor(ImmutableList.of(ItemValue.create(uid, card)));
		}

		eventProducer.vcardUpdated(uid);
		eventProducer.changed();
	}

	private static class Updated {
		String uid;
		long version;
		boolean dnChanged;

		public Updated(String u, long v, boolean dnChanged) {
			this.uid = u;
			this.version = v;
			this.dnChanged = dnChanged;
		}
	}

	private void refreshGroupsFor(Collection<ItemValue<VCard>> cards) {
		List<ItemValue<VCard>> groups = storeService
				.getMultiple(storeService.findGroupsContaining(cards.stream().map(c -> c.uid).toArray(String[]::new)));

		for (ItemValue<VCard> gv : groups) {
			gv.value.organizational.member.stream().forEach(m -> {
				cards.forEach(card -> {
					if ((m.containerUid == null || container.uid.equals(m.containerUid))
							&& card.uid.equals(m.itemUid)) {
						if (card.value == null) {
							m.containerUid = null;
						} else {
							m.commonName = getDisplayName(card.value);
							m.mailto = card.value.defaultMail();
						}
					}
				});
			});
			storeService.update(gv.uid, getDisplayName(gv.value), gv.value);
		}

	}

	/**
	 * Returns true if displayName or email has been changed, false otherwise
	 * 
	 * 
	 * @param uid
	 * @param card
	 * @param photo
	 * @return
	 * @throws ServerFault
	 */
	@SuppressWarnings("serial")
	private Updated doUpdate(String givenUid, Long itemId, VCard card, byte[] photo) throws ServerFault {

		ItemValue<VCard> previousItemValue = itemId == null ? storeService.get(givenUid, null)
				: storeService.get(itemId, null);
		if (previousItemValue == null || previousItemValue.value == null) {
			logger.error("VCard uid: {} doesn't exist !", givenUid);
			throw new ServerFault("VCard uid:" + givenUid + " doesn't exist !", ErrorCode.NOT_FOUND);
		}
		String uid = previousItemValue.uid;

		boolean emailEquals = card.defaultMail() == null ? previousItemValue.value.defaultMail() == null
				: card.defaultMail().equals(previousItemValue.value.defaultMail());
		boolean displayNameEquals = getDisplayName(card) == null ? getDisplayName(previousItemValue.value) == null
				: getDisplayName(card).equals(getDisplayName(previousItemValue.value));
		boolean directoryValueChanged = !displayNameEquals || !emailEquals;

		extSanitizer.update(previousItemValue.value, card, new HashMap<String, String>() {
			{
				put("containerUid", container.uid);
			}
		});

		extValidator.update(previousItemValue.value, card, ImmutableMap.of("containerUid", container.uid));

		ItemVersion upd = storeService.update(uid, getDisplayName(card), card);
		if (photo != null) {
			if (photo.length == 0) {
				storeService.deletePhoto(uid);
				storeService.deleteIcon(uid);
			} else {
				storeService.setPhoto(uid, photo);
				storeService.setIcon(uid, ImageUtils.resize(photo, 22, 22));
			}
		}
		return new Updated(uid, upd.version, directoryValueChanged);
	}

	private boolean doUpdateOrCreate(String uid, VCard card, byte[] photo) throws ServerFault {
		try {
			return doUpdate(uid, null, card, photo).dnChanged;
		} catch (ServerFault sf) {
			if (sf.getCode() == ErrorCode.NOT_FOUND) {
				logger.warn("vcard uid {} was sent as created but already exists. We update it", uid);
				doCreate(uid, null, card, photo);
				return false;
			} else {
				throw sf;
			}
		}
	}

	@Override
	public ItemValue<VCard> getComplete(String uid) throws ServerFault {
		rbacManager.check(Verb.Read.name());
		return storeService.get(uid, null);
	}

	@Override
	public ItemValue<VCard> getCompleteById(long uid) throws ServerFault {
		rbacManager.check(Verb.Read.name());
		return storeService.get(uid, null);
	}

	@Override
	public List<ItemValue<VCard>> multipleGet(List<String> uids) throws ServerFault {
		rbacManager.check(Verb.Read.name());
		return storeService.getMultiple(uids);
	}

	@Override
	public List<ItemValue<VCard>> multipleGetById(List<Long> ids) throws ServerFault {
		rbacManager.check(Verb.Read.name());
		return storeService.getMultipleById(ids);
	}

	@Override
	public ItemValue<VCardInfo> getInfo(String uid) throws ServerFault {
		rbacManager.check(Verb.Read.name());
		return adapt(storeService.get(uid, null));
	}

	private ItemValue<VCardInfo> adapt(ItemValue<VCard> item) {
		if (item == null)
			return null;

		return ItemValue.create(item, VCardInfo.create(item.value));
	}

	@Override
	public ListResult<ItemValue<VCardInfo>> search(VCardQuery query) throws ServerFault {
		rbacManager.check(Verb.Read.name());
		List<ItemValue<VCardInfo>> values = null;
		ListResult<ItemValue<VCardInfo>> ret = new ListResult<>();
		if (Strings.isNullOrEmpty(query.query)) {

			List<String> uids = storeService.allUidsOrderedByDisplayname();
			int from = query.from;
			int to = query.from + query.size;
			from = Math.min(from, uids.size());
			to = Math.min(to, uids.size());
			List<String> subUids = uids.subList(from, to);

			values = new ArrayList<>(subUids.size());
			ret.total = uids.size();

			List<ItemValue<VCard>> items = storeService.getMultiple(subUids);
			for (ItemValue<VCard> item : items) {
				values.add(adapt(item));
			}
		} else {
			ListResult<String> res = indexStore.search(query);

			values = new ArrayList<>(res.values.size());

			List<ItemValue<VCard>> items = storeService.getMultiple(res.values);
			for (ItemValue<VCard> item : items) {
				values.add(adapt(item));
			}
			ret.total = res.total;
		}
		if (query.orderBy != OrderBy.Pertinance) {
			Collections.sort(values, new Comparator<ItemValue<VCardInfo>>() {
				@Override
				public int compare(ItemValue<VCardInfo> o1, ItemValue<VCardInfo> o2) {
					return o1.displayName.compareToIgnoreCase(o2.displayName);
				}
			});
		}

		ret.values = values;
		return ret;
	}

	@Override
	public void delete(String uid) throws ServerFault {
		rbacManager.check(Verb.Write.name());
		doDelete(uid);
		refreshGroupsFor(ImmutableList.of(ItemValue.create(uid, null)));
		eventProducer.vcardDeleted(uid);
		eventProducer.changed();
	}

	private void doDelete(String uid) throws ServerFault {
		storeService.deletePhoto(uid);
		storeService.deleteIcon(uid);
		storeService.delete(uid);
	}

	@Override
	public ContainerChangelog containerChangelog(Long since) throws ServerFault {
		rbacManager.check(Verb.Read.name());
		return storeService.changelog(since, Long.MAX_VALUE);
	}

	@Override
	public ItemChangelog itemChangelog(String itemUid, Long since) throws ServerFault {
		rbacManager.check(Verb.Read.name());
		return ChangeLogUtil.getItemChangeLog(itemUid, since, context, storeService, container.domainUid);
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
	public ContainerUpdatesResult updates(VCardChanges changes) throws ServerFault {
		rbacManager.check(Verb.Write.name());
		boolean change = false;

		ContainerUpdatesResult ret = new ContainerUpdatesResult();
		ret.added = new ArrayList<String>();
		ret.updated = new ArrayList<String>();
		ret.removed = new ArrayList<String>();
		ret.errors = new ArrayList<>();

		List<ItemValue<VCard>> cards = new ArrayList<ItemValue<VCard>>();
		if (changes.add != null && changes.add.size() > 0) {
			change = true;
			Consumer<ItemAdd> createOrUpdate = add -> {
				try {
					boolean importantChanges = doCreateOrUpdate(add.uid, add.value, add.photo);
					if (importantChanges) {
						cards.add(ItemValue.create(add.uid, add.value));
					}
					ret.added.add(add.uid);
				} catch (ValidationException ve) {
					ret.errors.add(ContainerUpdatesResult.InError.create(ve.getMessage(), ve.getCode(), add.uid));
					logger.error(ve.getMessage());
				} catch (ServerFault sf) {
					ret.errors.add(ContainerUpdatesResult.InError.create(sf.getMessage(), sf.getCode(), add.uid));
					logger.error(sf.getMessage(), sf);
				}

			};

			Map<Boolean, List<ItemAdd>> isGroupPartitions = changes.add.stream()
					.collect(Collectors.partitioningBy(c -> c.value.kind == VCard.Kind.group));
			isGroupPartitions.get(false).forEach(createOrUpdate);

			DependencyResolver.sortByDependencies(isGroupPartitions.get(true), itemAdd -> itemAdd.uid,
					itemAdd -> itemAdd.value.organizational.member.stream()
							.filter(member -> (member.containerUid == null || member.containerUid.equals(container.uid))
									&& member.itemUid != null)
							.map(member -> member.itemUid).collect(Collectors.toSet()))
					.forEach(createOrUpdate);
		}

		if (changes.modify != null && changes.modify.size() > 0) {

			change = true;

			for (VCardChanges.ItemModify update : changes.modify) {
				try {
					boolean importantChanges = doUpdateOrCreate(update.uid, update.value, update.photo);
					if (importantChanges) {
						cards.add(ItemValue.create(update.uid, update.value));
					}
					ret.updated.add(update.uid);
				} catch (ValidationException ve) {
					ret.errors.add(ContainerUpdatesResult.InError.create(ve.getMessage(), ve.getCode(), update.uid));
					logger.error(ve.getMessage());
				} catch (ServerFault sf) {
					ret.errors.add(ContainerUpdatesResult.InError.create(sf.getMessage(), sf.getCode(), update.uid));
					logger.error(sf.getMessage(), sf);
				}

			}
		}

		if (changes.delete != null && changes.delete.size() > 0) {
			change = true;
			for (VCardChanges.ItemDelete item : changes.delete) {
				try {
					doDelete(item.uid);
					cards.add(ItemValue.create(item.uid, null));
					ret.removed.add(item.uid);
				} catch (ServerFault sf) {
					if (sf.getCode() == ErrorCode.NOT_FOUND) {
						logger.warn("vcard uid {} was sent as deleted but does not exist.", item.uid);
						ret.removed.add(item.uid);
					} else {
						ret.errors.add(ContainerUpdatesResult.InError.create(sf.getMessage(), sf.getCode(), item.uid));
						logger.error(sf.getMessage(), sf);
					}
				}

			}

		}

		refreshGroupsFor(cards);

		ret.version = storeService.getVersion();
		if (change) {
			eventProducer.changed();
			logger.info("{} batch updates: {} added, {} updated, {} removed.", container.uid, ret.added.size(),
					ret.updated.size(), ret.removed.size());
		} else {
			logger.info("Empty batch of updates.");
		}
		return ret;
	}

	@Override
	public ContainerChangeset<String> sync(Long since, VCardChanges changes) throws ServerFault {
		updates(changes);
		return changeset(since);
	}

	private String getDisplayName(VCard card) {
		if (card.identification.formatedName != null) {
			return card.identification.formatedName.value;
		} else {
			return null;
		}
	}

	@Override
	public void copy(List<String> uids, String descContainerUid) throws ServerFault {
		rbacManager.check(Verb.Read.name());
		IAddressBook dest = ServerSideServiceProvider.getProvider(securityContext).instance(IAddressBook.class,
				descContainerUid);

		for (String uid : uids) {

			ItemValue<VCard> value = getComplete(uid);
			System.out.println("copy " + uid + " value " + value);
			if (value != null) {
				copyVCard(dest, uid, value);
			}
		}
	}

	@Override
	public void move(List<String> uids, String descContainerUid) throws ServerFault {
		rbacManager.check(Verb.Write.name());
		IAddressBook dest = ServerSideServiceProvider.getProvider(securityContext).instance(IAddressBook.class,
				descContainerUid);

		for (String uid : uids) {
			ItemValue<VCard> value = getComplete(uid);
			if (value != null) {
				copyVCard(dest, uid, value);
				delete(uid);
			}
		}

	}

	private void copyVCard(IAddressBook dest, String uid, ItemValue<VCard> value) throws ServerFault {
		dest.create(uid, value.value);
		byte[] photo = getPhoto(uid);
		if (null != photo) {
			dest.setPhoto(uid, photo);
		}
	}

	public List<ItemValue<VCard>> all() throws ServerFault {
		rbacManager.check(Verb.Read.name());
		return storeService.all();
	}

	@Override
	public void setPhoto(String uid, byte[] photo) throws ServerFault {
		logger.info("setPhoto on {}", uid);
		rbacManager.check(Verb.Write.name());

		ItemValue<VCard> item = storeService.get(uid, null);
		if (item == null) {
			throw new ServerFault("item doesnt exists", ErrorCode.NOT_FOUND);
		}

		byte[] asPng = ImageUtils.checkAndSanitize(photo);
		storeService.setPhoto(uid, asPng);
		byte[] icon = ImageUtils.resize(asPng, 22, 22);
		storeService.setIcon(uid, icon);
	}

	@Override
	public void deletePhoto(String uid) throws ServerFault {
		rbacManager.check(Verb.Write.name());
		ItemValue<VCard> item = storeService.get(uid, null);
		if (item == null) {
			throw new ServerFault("item doesnt exists", ErrorCode.NOT_FOUND);
		}
		storeService.deletePhoto(uid);
		storeService.deleteIcon(uid);
	}

	@Override
	public byte[] getPhoto(String uid) throws ServerFault {
		// FIXME accessManager.checkReadAccess();
		return storeService.getPhoto(uid);
	}

	@Override
	public byte[] getIcon(String uid) throws ServerFault {
		// FIXME accessManager.checkReadAccess();
		byte[] ret = storeService.getIcon(uid);
		if (ret == null) {
			ret = AddressBookResources.DEFAULT_INDIV_ICON;
		}
		return ret;
	}

	@Override
	public List<String> findByEmail(String email) throws ServerFault {
		rbacManager.check(Verb.Read.name());
		return storeService.findByEmail(email);
	}

	@Override
	public void reset() throws ServerFault {
		rbacManager.check(Verb.Manage.name());
		storeService.deleteAll();
		eventProducer.changed();

		ContainerSyncStatus status = new ContainerSyncStatus();
		status.nextSync = 0L;
		new ContainerSyncStore(DataSourceRouter.get(context, container.uid), container).setSyncStatus(status);

	}

	@Override
	public long getVersion() throws ServerFault {
		rbacManager.check(Verb.Read.name());
		return storeService.getVersion();
	}

	public Ack updateById(long id, VCard card) {
		rbacManager.check(Verb.Write.name());

		Updated upd = doUpdate(null, id, card, null);
		refreshGroupsFor(ImmutableList.of(ItemValue.create(upd.uid, card)));
		eventProducer.vcardUpdated(upd.uid);
		eventProducer.changed();
		return Ack.create(upd.version);
	}

	@Override
	public void deleteById(long id) {
		ItemValue<VCard> withUid = getCompleteById(id);
		if (withUid != null) {
			delete(withUid.uid);
		}
	}

	@Override
	public Count count(ItemFlagFilter filter) throws ServerFault {
		rbacManager.check(Verb.Read.name());
		return storeService.count(filter);
	}

	@Override
	public List<Long> sortedIds(SortDescriptor sorted) throws ServerFault {
		rbacManager.check(Verb.Read.name());
		try {
			return vcardStore.sortedIds(sorted);
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

		storeService.xfer(ds, c, new VCardStore(ds, c));

	}

	@Override
	public void multipleDeleteById(List<Long> ids) throws ServerFault {
		ids.forEach(id -> deleteById(id));
	}

}
