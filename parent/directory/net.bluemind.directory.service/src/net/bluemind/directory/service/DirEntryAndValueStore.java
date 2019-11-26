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
package net.bluemind.directory.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.persistence.VCardStore;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.persistence.IItemValueStore;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.persistence.DirEntryStore;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.persistence.MailFilterStore;
import net.bluemind.mailbox.persistence.MailboxStore;

public class DirEntryAndValueStore<T> implements IItemValueStore<net.bluemind.directory.service.DirEntryAndValue<T>> {

	private IItemValueStore<T> valueStore;
	private DirEntryStore dirEntryStore;
	private VCardStore vcardStore;
	private MailboxStore mailboxStore;
	private MailFilterStore mailFilterStore;

	public DirEntryAndValueStore(DataSource ds, Container container, IItemValueStore<T> itemValueStore) {
		this.dirEntryStore = new DirEntryStore(ds, container);
		this.valueStore = itemValueStore;
		this.vcardStore = new VCardStore(ds, container);
		this.mailboxStore = new MailboxStore(ds, container);
		this.mailFilterStore = new MailFilterStore(ds, container);
	}

	@Override
	public void create(Item item, DirEntryAndValue<T> value) throws SQLException {
		dirEntryStore.create(item, value.entry);
		if (value.vcard != null) {
			vcardStore.create(item, value.vcard);
		}

		if (value.mailbox != null) {
			mailboxStore.create(item, value.mailbox);
		}

		if (valueStore != null) {
			valueStore.create(item, value.value);
		}
	}

	@Override
	public void update(Item item, DirEntryAndValue<T> value) throws SQLException {
		dirEntryStore.update(item, value.entry);
		if (value.vcard != null) {
			vcardStore.update(item, value.vcard);
		}
		if (value.mailbox != null) {
			mailboxStore.update(item, value.mailbox);
		}

		if (valueStore != null) {
			valueStore.update(item, value.value);
		}
	}

	@Override
	public void delete(Item item) throws SQLException {
		dirEntryStore.delete(item);
		vcardStore.delete(item);
		mailFilterStore.delete(item);
		mailboxStore.delete(item);
		if (valueStore != null) {
			valueStore.delete(item);
		}
	}

	@Override
	public DirEntryAndValue<T> get(Item item) throws SQLException {
		DirEntry dirEntry = dirEntryStore.get(item);
		VCard vcard = vcardStore.get(item);
		Mailbox mailbox = mailboxStore.get(item);
		T value = null;
		if (valueStore != null) {
			value = valueStore.get(item);
		}
		return new DirEntryAndValue<>(dirEntry, value, vcard, mailbox);
	}

	@Override
	public void deleteAll() throws SQLException {
		throw new SQLException("DO NOT CALL THIS METHOD");
	}

	@Override
	public List<DirEntryAndValue<T>> getMultiple(List<Item> items) throws SQLException {
		List<DirEntryAndValue<T>> ret = new ArrayList<>(items.size());
		for (Item i : items) {
			ret.add(get(i));
		}

		return ret;
	}

}
