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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.elasticsearch.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

import net.bluemind.addressbook.api.IAddressBook;
import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Organizational.Member;
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
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.ContainerSyncStatus;
import net.bluemind.core.container.model.ContainerUpdatesResult;
import net.bluemind.core.container.model.Item;
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

	private static final int MAX_SIZE = 15000;

	private static final String CONTAINER_UID_PARAM = "containerUid";

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

		this.indexStore = new VCardIndexStore(esearchClient, container,
				DataSourceRouter.location(context, container.uid));
		this.storeService = new VCardContainerStoreService(context, dataSource, securityContext, container, vcardStore,
				indexStore);

		extSanitizer = new Sanitizer(context);
		extValidator = new Validator(context);

		rbacManager = RBACManager.forContext(context).forContainer(container);
	}

	@Override
	public List<String> allUids() {
		rbacManager.check(Verb.Read.name());
		return storeService.allUids();
	}

	@Override
	public void create(String uid, VCard card) {
		rbacManager.check(Verb.Write.name());
		Item item = Item.create(uid, null);
		createAndNotify(item, card);
	}

	@Override
	public Ack createById(long id, VCard card) {
		Item item = Item.create("vcard-by-id:" + id, id);
		ItemVersion version = createAndNotify(item, card);
		return version.ack();
	}

	private ItemVersion createAndNotify(Item item, VCard card) {
		rbacManager.check(Verb.Write.name());
		ItemVersion version = doCreate(item, card, null);
		eventProducer.vcardCreated(item.uid);
		emitNotification();
		logger.info("createdById {} ({}) => v{}", item.id, item.uid, version);
		return version;
	}

	private ItemVersion doCreate(Item item, VCard card, byte[] photo) {

		if (!isDomainAddressbook() && storeService.getItemCount() >= MAX_SIZE) {
			throw new ServerFault("Max items count in addressbook exceeded", ErrorCode.MAX_ITEM_COUNT);
		}

		// ext point sanitizer
		extSanitizer.create(card, Map.of(CONTAINER_UID_PARAM, container.uid));
		extValidator.create(card, Map.of(CONTAINER_UID_PARAM, container.uid));

		item.displayName = getDisplayName(card);
		ItemVersion version = storeService.create(item, card);
		if (photo != null) {
			if (photo.length == 0) {
				storeService.deletePhoto(item.uid);
				storeService.deleteIcon(item.uid);
			} else {
				storeService.setPhoto(item.uid, photo);
				storeService.setIcon(item.uid, ImageUtils.resize(photo, 22, 22));
			}
		}
		return version;
	}

	private boolean doCreateOrUpdate(String uid, VCard value, byte[] photo) {
		Item item = Item.create(uid, null);
		try {
			doCreate(item, value, photo);
			return false;
		} catch (ServerFault sf) {
			if (sf.getCode() == ErrorCode.ALREADY_EXISTS) {
				logger.warn("vcard uid {} was sent as created but already exists. We update it", uid);
				return doUpdate(item, value, photo).dnChanged;
			} else {
				throw sf;
			}
		}
	}

	@Override
	public void update(String uid, VCard card) {
		Item item = Item.create(uid, null);
		updateAndNotify(item, card);
	}

	@Override
	public Ack updateById(long id, VCard card) {
		Item item = Item.create(null, id);
		ItemVersion version = updateAndNotify(item, card);
		return version.ack();
	}

	private ItemVersion updateAndNotify(Item item, VCard card) {
		rbacManager.check(Verb.Write.name());
		Updated upd = doUpdate(item, card, null);
		if (upd.dnChanged) {
			refreshGroupsFor(ImmutableList.of(ItemValue.create(upd.uid, card)));
		}
		eventProducer.vcardUpdated(upd.uid);
		emitNotification();
		return upd.version;
	}

	private static class Updated {
		String uid;
		ItemVersion version;
		boolean dnChanged;

		public Updated(String u, ItemVersion v, boolean dnChanged) {
			this.uid = u;
			this.version = v;
			this.dnChanged = dnChanged;
		}
	}

	private void refreshGroupsFor(Collection<ItemValue<VCard>> cards) {
		List<ItemValue<VCard>> groups = storeService
				.getMultiple(storeService.findGroupsContaining(cards.stream().map(c -> c.uid).toArray(String[]::new)));

		groups.forEach(groupItem -> refreshGroupFor(groupItem, cards));
	}

	private void refreshGroupFor(ItemValue<VCard> groupItem, Collection<ItemValue<VCard>> vcardItems) {
		groupItem.value.organizational.member //
				.forEach(member -> vcardItems.stream() //
						.filter(vcardItem -> vCardMatchingDListMember(vcardItem, member)) //
						.forEach(vcardItem -> {
							if (vcardItem.value == null) {
								// cardItem has been deleted
								member.containerUid = null;
							} else {
								// cardItem has been updated
								member.commonName = getDisplayName(vcardItem.value);
								if (!VCardGroupSanitizer.isVCardMemberEmailValid(member, vcardItem.value)) {
									member.mailto = vcardItem.value.defaultMail();
								}
							}
						}));

		storeService.update(groupItem.uid, getDisplayName(groupItem.value), groupItem.value);
		eventProducer.vcardUpdated(groupItem.uid);
	}

	private boolean vCardMatchingDListMember(ItemValue<VCard> vcardItem, Member member) {
		return (member.containerUid == null || container.uid.equals(member.containerUid))
				&& vcardItem.uid.equals(member.itemUid);
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
	private Updated doUpdate(Item item, VCard card, byte[] photo) {
		ItemValue<VCard> previousItemValue = item.uid != null ? storeService.get(item.uid, null)
				: storeService.get(item.id, null);
		if (previousItemValue == null || previousItemValue.value == null) {
			logger.error("VCard uid: {} doesn't exist !", item.uid);
			throw new ServerFault("VCard uid:" + item.uid + " doesn't exist !", ErrorCode.NOT_FOUND);
		}
		item.uid = previousItemValue.uid;

		boolean emailEquals = card.defaultMail() == null ? previousItemValue.value.defaultMail() == null
				: card.defaultMail().equals(previousItemValue.value.defaultMail());

		boolean displayNameEquals = Objects.equals(getDisplayName(card), getDisplayName(previousItemValue.value));
		boolean directoryValueChanged = !displayNameEquals || !emailEquals;

		extSanitizer.update(previousItemValue.value, card, Map.of(CONTAINER_UID_PARAM, container.uid));
		extValidator.update(previousItemValue.value, card, Map.of(CONTAINER_UID_PARAM, container.uid));

		ItemVersion upd = storeService.update(item, getDisplayName(card), card);
		if (photo != null) {
			if (photo.length == 0) {
				storeService.deletePhoto(item.uid);
				storeService.deleteIcon(item.uid);
			} else {
				storeService.setPhoto(item.uid, photo);
				storeService.setIcon(item.uid, ImageUtils.resize(photo, 22, 22));
			}
		}
		return new Updated(item.uid, upd, directoryValueChanged);
	}

	private boolean doUpdateOrCreate(String uid, VCard card, byte[] photo) {
		Item item = Item.create(uid, null);
		item.displayName = getDisplayName(card);
		try {
			return doUpdate(item, card, photo).dnChanged;
		} catch (ServerFault sf) {
			if (sf.getCode() == ErrorCode.NOT_FOUND) {
				logger.warn("vcard uid {} was sent as created but already exists. We update it", uid);
				doCreate(item, card, photo);
				return false;
			} else {
				throw sf;
			}
		}
	}

	@Override
	public ItemValue<VCard> getComplete(String uid) {
		rbacManager.check(Verb.Read.name());
		return storeService.get(uid, null);
	}

	@Override
	public ItemValue<VCard> getCompleteById(long uid) {
		rbacManager.check(Verb.Read.name());
		return storeService.get(uid, null);
	}

	@Override
	public List<ItemValue<VCard>> multipleGet(List<String> uids) {
		rbacManager.check(Verb.Read.name());
		return storeService.getMultiple(uids);
	}

	@Override
	public List<ItemValue<VCard>> multipleGetById(List<Long> ids) {
		rbacManager.check(Verb.Read.name());
		return storeService.getMultipleById(ids);
	}

	@Override
	public ItemValue<VCardInfo> getInfo(String uid) {
		rbacManager.check(Verb.Read.name());
		return adapt(storeService.get(uid, null));
	}

	private ItemValue<VCardInfo> adapt(ItemValue<VCard> item) {
		if (item == null)
			return null;

		return ItemValue.create(item, VCardInfo.create(item.value));
	}

	@Override
	public ListResult<ItemValue<VCardInfo>> search(VCardQuery query) {
		rbacManager.check(Verb.Read.name());
		ListResult<ItemValue<VCardInfo>> ret = new ListResult<>();
		ListResult<String> res = indexStore.search(query);

		List<ItemValue<VCardInfo>> values = storeService.getMultiple(res.values).stream().map(this::adapt)
				.collect(Collectors.toList());

		if (query.orderBy != OrderBy.Pertinance) {
			Collections.sort(values, (ItemValue<VCardInfo> o1, ItemValue<VCardInfo> o2) -> o1.displayName
					.compareToIgnoreCase(o2.displayName));
		}

		ret.total = res.total;
		ret.values = values;
		return ret;
	}

	@Override
	public void delete(String uid) {
		rbacManager.check(Verb.Write.name());
		doDelete(uid);
		refreshGroupsFor(List.of(ItemValue.create(uid, null)));
		eventProducer.vcardDeleted(uid);
		emitNotification();
	}

	private void doDelete(String uid) {
		storeService.deletePhoto(uid);
		storeService.deleteIcon(uid);
		storeService.delete(uid);
	}

	@Override
	public ItemChangelog itemChangelog(String itemUid, Long since) {
		rbacManager.check(Verb.Read.name());
		return ChangeLogUtil.getItemChangeLog(itemUid, since, context, storeService, container.domainUid);
	}

	@Override
	public ContainerChangeset<String> changeset(Long since) {
		rbacManager.check(Verb.Read.name());
		return storeService.changeset(since, Long.MAX_VALUE);
	}

	@Override
	public ContainerChangeset<Long> changesetById(Long since) {
		rbacManager.check(Verb.Read.name());
		return storeService.changesetById(since, Long.MAX_VALUE);
	}

	@Override
	public ContainerChangeset<ItemVersion> filteredChangesetById(Long since, ItemFlagFilter filter) {
		rbacManager.check(Verb.Read.name());
		return storeService.changesetById(since, filter);
	}

	@Override
	public ContainerUpdatesResult updates(VCardChanges changes) {
		rbacManager.check(Verb.Write.name());
		boolean change = false;

		int current = storeService.getItemCount();
		int added = changes.add != null ? changes.add.size() : 0;
		int deleted = changes.delete != null ? changes.delete.size() : 0;

		int newSize = current + added - deleted;

		if (!isDomainAddressbook() && newSize > MAX_SIZE) {
			ContainerUpdatesResult ret = new ContainerUpdatesResult();
			ret.errors = new ArrayList<>();
			ret.errors.add(ContainerUpdatesResult.InError.create("Max items count in addressbook exceeded",
					ErrorCode.MAX_ITEM_COUNT, null));
			return ret;
		}

		ContainerUpdatesResult ret = new ContainerUpdatesResult();
		ret.added = new ArrayList<String>();
		ret.updated = new ArrayList<String>();
		ret.removed = new ArrayList<String>();
		ret.errors = new ArrayList<>();

		List<ItemValue<VCard>> cards = new ArrayList<>();
		if (changes.add != null && !changes.add.isEmpty()) {
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

		if (changes.modify != null && !changes.modify.isEmpty()) {

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

		if (changes.delete != null && !changes.delete.isEmpty()) {
			change = true;
			for (VCardChanges.ItemDelete item : changes.delete) {
				try {
					doDelete(item.uid);
					cards.add(ItemValue.create(item.uid, null));
					ret.removed.add(item.uid);
				} catch (ServerFault sf) {
					if (sf.getCode() == ErrorCode.NOT_FOUND) {
						logger.warn("vcard uid {} was sent as deleted but does not exist.", item.uid);
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
			emitNotification();
			logger.info("{} batch updates: {} added, {} updated, {} removed.", container.uid, ret.added.size(),
					ret.updated.size(), ret.removed.size());
		} else {
			logger.info("Empty batch of updates.");
		}
		return ret;
	}

	@Override
	public ContainerChangeset<String> sync(Long since, VCardChanges changes) {
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
	public void copy(List<String> uids, String descContainerUid) {
		rbacManager.check(Verb.Read.name());
		IAddressBook dest = ServerSideServiceProvider.getProvider(securityContext).instance(IAddressBook.class,
				descContainerUid);

		for (String uid : uids) {

			ItemValue<VCard> value = getComplete(uid);
			logger.debug("copy {} value {}", uid, value);
			if (value != null) {
				copyVCard(dest, uid, value);
			}
		}
	}

	@Override
	public void move(List<String> uids, String descContainerUid) {
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

	private void copyVCard(IAddressBook dest, String uid, ItemValue<VCard> value) {
		dest.create(uid, value.value);
		byte[] photo = getPhoto(uid);
		if (null != photo) {
			dest.setPhoto(uid, photo);
		}
	}

	public List<ItemValue<VCard>> all() {
		rbacManager.check(Verb.Read.name());
		return storeService.all();
	}

	@Override
	public void setPhoto(String uid, byte[] photo) {
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
	public void deletePhoto(String uid) {
		rbacManager.check(Verb.Write.name());
		ItemValue<VCard> item = storeService.get(uid, null);
		if (item == null) {
			throw new ServerFault("item doesnt exists", ErrorCode.NOT_FOUND);
		}
		storeService.deletePhoto(uid);
		storeService.deleteIcon(uid);
	}

	@Override
	public byte[] getPhoto(String uid) {
		// FIXME accessManager.checkReadAccess();
		return storeService.getPhoto(uid);
	}

	@Override
	public byte[] getIcon(String uid) {
		// FIXME accessManager.checkReadAccess();
		byte[] ret = storeService.getIcon(uid);
		if (ret == null) {
			ret = AddressBookResources.DEFAULT_INDIV_ICON;
		}
		return ret;
	}

	@Override
	public List<String> findByEmail(String email) {
		rbacManager.check(Verb.Read.name());
		return storeService.findByEmail(email);
	}

	@Override
	public void reset() {
		rbacManager.check(Verb.Manage.name());
		storeService.deleteAll();
		eventProducer.changed();

		ContainerSyncStatus status = new ContainerSyncStatus();
		status.nextSync = 0L;
		new ContainerSyncStore(DataSourceRouter.get(context, container.uid), container).setSyncStatus(status);

	}

	@Override
	public long getVersion() {
		rbacManager.check(Verb.Read.name());
		return storeService.getVersion();
	}

	@Override
	public void deleteById(long id) {
		ItemValue<VCard> withUid = getCompleteById(id);
		if (withUid != null) {
			delete(withUid.uid);
		}
	}

	@Override
	public Count count(ItemFlagFilter filter) {
		rbacManager.check(Verb.Read.name());
		return storeService.count(filter);
	}

	@Override
	public List<Long> sortedIds(SortDescriptor sorted) {
		rbacManager.check(Verb.Read.name());
		try {
			return vcardStore.sortedIds(sorted);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public void xfer(String serverUid) {
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
	public void multipleDeleteById(List<Long> ids) {
		ids.forEach(this::deleteById);
	}

	private boolean isDomainAddressbook() {
		return container.uid.equals(container.owner);
	}

	public void emitNotification() {
		indexStore.refresh();
		eventProducer.changed();
	}

	@Override
	public VCard get(String uid) {
		ItemValue<VCard> item = getComplete(uid);
		return item != null ? item.value : null;
	}

	@Override
	public void restore(ItemValue<VCard> cardItem, boolean isCreate) {
		if (isCreate) {
			createAndNotify(cardItem.item(), cardItem.value);
		} else {
			updateAndNotify(cardItem.item(), cardItem.value);
		}
	}

	@Override
	public void touch(String uid) {
		rbacManager.check(Verb.Write.name());
		storeService.touch(uid);
	}
}
