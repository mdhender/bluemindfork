/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.smime.cacerts.service.internal;

import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ErrorCode;
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
import net.bluemind.core.container.service.internal.ContainerStoreService;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.sanitizer.Sanitizer;
import net.bluemind.core.validator.Validator;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.smime.cacerts.api.ISmimeCACert;
import net.bluemind.smime.cacerts.api.ISmimeCacertUids;
import net.bluemind.smime.cacerts.api.SmimeCacert;
import net.bluemind.smime.cacerts.persistence.SmimeCacertStore;

public class SmimeCACertService implements ISmimeCACert {

	private static final Logger logger = LoggerFactory.getLogger(SmimeCACertService.class);
	private ContainerStoreService<SmimeCacert> storeService;
	private SmimeCacertSanitizer sanitizer;
	private SmimeCacertValidator validator;
	private Sanitizer extSanitizer;
	private Validator extValidator;
	private BmContext bmContext;
	private Container container;
	private RBACManager rbacManager;

	public SmimeCACertService(DataSource pool, Container container, BmContext bmContext) {
		this.bmContext = bmContext;
		this.container = container;

		storeService = new ContainerStoreService<>(pool, bmContext.getSecurityContext(), container,
				new SmimeCacertStore(pool, container));

		sanitizer = new SmimeCacertSanitizer();

		validator = new SmimeCacertValidator();

		extSanitizer = new Sanitizer(bmContext);
		extValidator = new Validator(bmContext);
		rbacManager = RBACManager.forContext(bmContext).forContainer(container);
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
	public ItemValue<SmimeCacert> getCompleteById(long id) {
		return storeService.get(id, null);
	}

	@Override
	public List<ItemValue<SmimeCacert>> multipleGetById(List<Long> ids) {
		rbacManager.check(Verb.Read.name());
		return storeService.getMultipleById(ids);
	}

	private ItemVersion create(Item item, SmimeCacert cert) {
		rbacManager.check(BasicRoles.ROLE_MANAGE_DOMAIN_SMIME);
		ItemVersion version = doCreate(item, cert);
		return version;
	}

	private ItemVersion doCreate(Item item, SmimeCacert cert) throws ServerFault {
		ItemVersion iv = storeService.create(item, cert);
		return iv;
	}

	private ItemVersion update(Item item, SmimeCacert cert) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_DOMAIN_SMIME);
		ItemVersion version = doUpdate(item, cert);
		return version;
	}

	private ItemVersion doUpdate(Item item, SmimeCacert cert) throws ServerFault {
		ItemValue<SmimeCacert> previousItemValue = storeService.get(item.uid, null);
		if (previousItemValue == null || previousItemValue.value == null) {
			throw new ServerFault("S/MIME certificate uid:" + item.uid + " doesn't exist !", ErrorCode.NOT_FOUND);
		}
		sanitizer.update(previousItemValue.value, cert);
		extSanitizer.update(previousItemValue.value, cert);
		validator.update(previousItemValue.value, cert);
		extValidator.update(previousItemValue.value, cert);

		ItemVersion version = storeService.update(item, cert.cert, cert);
		return version;
	}

	@Override
	public List<String> allUids() {
		rbacManager.check(Verb.Read.name());
		return storeService.allUids();
	}

	@Override
	public Ack create(String uid, SmimeCacert cert) {
		rbacManager.check(BasicRoles.ROLE_MANAGE_DOMAIN_SMIME);
		sanitizer.create(cert);
		extSanitizer.create(cert);
		validator.create(cert);
		extValidator.create(cert);
		Item item = Item.create(uid, null);
		item.displayName = ISmimeCacertUids.TYPE.concat("_").concat(uid);
		ItemVersion version = create(item, cert);
		logger.info("Created ItemVersion {}", version.version);
		return Ack.create(version.version);
	}

	@Override
	public Ack update(String uid, SmimeCacert value) {
		rbacManager.check(BasicRoles.ROLE_MANAGE_DOMAIN_SMIME);
		Item item = Item.create(uid, null);
		item.displayName = ISmimeCacertUids.TYPE.concat("_").concat(uid);
		ItemVersion version = update(item, value);
		logger.info("Updated ItemVersion {}", version.version);
		return Ack.create(version.version);
	}

	@Override
	public void delete(String uid) {
		rbacManager.check(BasicRoles.ROLE_MANAGE_DOMAIN_SMIME);
		if (getComplete(uid) == null) {
			throw new ServerFault("item doesnt exists", ErrorCode.NOT_FOUND);
		} else {
			storeService.delete(uid);
		}
	}

	@Override
	public void deleteAll() {
		rbacManager.check(BasicRoles.ROLE_MANAGE_DOMAIN_SMIME);
		storeService.deleteAll();
	}

	@Override
	public ItemValue<SmimeCacert> getComplete(String uid) {
		rbacManager.check(Verb.Read.name());
		return storeService.get(uid, null);
	}

	@Override
	public List<ItemValue<SmimeCacert>> multipleGet(List<String> uids) {
		rbacManager.check(Verb.Read.name());
		return storeService.getMultiple(uids);
	}

	@Override
	public List<ItemValue<SmimeCacert>> all() throws ServerFault {
		rbacManager.check(Verb.Read.name());
		return storeService.all();
	}

	@Override
	public void reset() throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_DOMAIN_SMIME);
		storeService.deleteAll();
	}

	@Override
	public SmimeCacert get(String uid) {
		rbacManager.check(Verb.Read.name());
		ItemValue<SmimeCacert> item = getComplete(uid);
		return item != null ? item.value : null;
	}

	@Override
	public void restore(ItemValue<SmimeCacert> item, boolean isCreate) {
		rbacManager.check(BasicRoles.ROLE_MANAGE_DOMAIN_SMIME);
		if (isCreate) {
			create(item.item(), item.value);
		} else {
			update(item.item(), item.value);
		}
	}

}
