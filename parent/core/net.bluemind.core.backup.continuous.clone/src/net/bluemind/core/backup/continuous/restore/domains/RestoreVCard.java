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
package net.bluemind.core.backup.continuous.restore.domains;

import com.fasterxml.jackson.core.type.TypeReference;

import net.bluemind.addressbook.api.IAddressBook;
import net.bluemind.addressbook.api.IAddressBookUids;
import net.bluemind.addressbook.api.VCard;
import net.bluemind.core.backup.continuous.DataElement;
import net.bluemind.core.backup.continuous.RecordKey;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.core.utils.JsonUtils.ValueReader;

public class RestoreVCard implements RestoreDomainType {

	private static final ValueReader<ItemValue<VCard>> mrReader = JsonUtils
			.reader(new TypeReference<ItemValue<VCard>>() {
			});
	private final IServerTaskMonitor monitor;
	private IServiceProvider target;

	public RestoreVCard(IServerTaskMonitor monitor, IServiceProvider target) {
		this.monitor = monitor;
		this.target = target;
	}

	@Override
	public String type() {
		return IAddressBookUids.TYPE;
	}

	@Override
	public void restore(DataElement de) {
		String payload = new String(de.payload);
		IAddressBook bookApi = target.instance(IAddressBook.class, de.key.uid);
		if (de.payload.length > 0) {
			createOrUpdate(payload, bookApi);
		} else {
			delete(de.key, bookApi);
		}

	}

	private void createOrUpdate(String payload, IAddressBook bookApi) {
		ItemValue<VCard> item = mrReader.read(payload);
		ItemValue<VCard> existing = bookApi.getCompleteById(item.internalId);
		if (existing != null) {
			bookApi.updateWithItem(item);
			monitor.log("Update VCard '" + item.displayName + "'");
		} else {
			bookApi.createWithItem(item);
			monitor.log("Create VCard '" + item.displayName + "'");
		}
	}

	private void delete(RecordKey key, IAddressBook bookApi) {
		try {
			bookApi.deleteById(key.id);
		} catch (Exception e) {
			monitor.log("Fails to delete resourceTypes: " + key);
		}
	}

}
