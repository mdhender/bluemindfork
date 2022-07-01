/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.core.backup.continuous.restore.domains.crud;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.core.type.TypeReference;

import net.bluemind.addressbook.api.AddressBookDescriptor;
import net.bluemind.addressbook.api.IAddressBook;
import net.bluemind.addressbook.api.IAddressBookUids;
import net.bluemind.addressbook.api.IAddressBooksMgmt;
import net.bluemind.addressbook.api.VCard;
import net.bluemind.core.backup.continuous.RecordKey;
import net.bluemind.core.backup.continuous.dto.VersionnedItem;
import net.bluemind.core.backup.continuous.restore.domains.RestoreLogger;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.core.utils.JsonUtils.ValueReader;
import net.bluemind.domain.api.Domain;

public class RestoreVCard extends CrudItemRestore<VCard> {

	private static final ValueReader<VersionnedItem<VCard>> reader = JsonUtils
			.reader(new TypeReference<VersionnedItem<VCard>>() {
			});
	private final IServiceProvider target;

	Set<String> validatedBooks = ConcurrentHashMap.newKeySet();

	public RestoreVCard(RestoreLogger log, ItemValue<Domain> domain, IServiceProvider target) {
		super(log, domain);
		this.target = target;
	}

	@Override
	public String type() {
		return IAddressBookUids.TYPE;
	}

	@Override
	protected ValueReader<VersionnedItem<VCard>> reader() {
		return reader;
	}

	@Override
	protected IAddressBook api(ItemValue<Domain> domain, RecordKey key) {
		if (!validatedBooks.contains(key.uid)) {
			IContainers contApi = target.instance(IContainers.class);
			if (contApi.getIfPresent(key.uid) == null) {
				IAddressBooksMgmt mgmtApi = target.instance(IAddressBooksMgmt.class);
				mgmtApi.create(key.uid, AddressBookDescriptor.create("book-" + key.uid, key.owner, domain.uid), false);
				validatedBooks.add(key.uid);
			}
		}

		return target.instance(IAddressBook.class, key.uid);
	}

}
