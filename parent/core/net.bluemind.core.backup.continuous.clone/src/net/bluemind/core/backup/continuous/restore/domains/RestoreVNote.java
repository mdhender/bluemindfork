/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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

import net.bluemind.core.backup.continuous.DataElement;
import net.bluemind.core.backup.continuous.RecordKey;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.core.utils.JsonUtils.ValueReader;
import net.bluemind.notes.api.INote;
import net.bluemind.notes.api.INoteUids;
import net.bluemind.notes.api.VNote;

public class RestoreVNote implements RestoreDomainType {

	private static final ValueReader<ItemValue<VNote>> mrReader = JsonUtils
			.reader(new TypeReference<ItemValue<VNote>>() {
			});
	private final IServerTaskMonitor monitor;
	private IServiceProvider target;

	public RestoreVNote(IServerTaskMonitor monitor, IServiceProvider target) {
		this.monitor = monitor;
		this.target = target;
	}

	@Override
	public String type() {
		return INoteUids.TYPE;
	}

	@Override
	public void restore(DataElement de) {
		String payload = new String(de.payload);
		INote noteApi = target.instance(INote.class, de.key.uid);
		if (de.payload.length > 0) {
			createOrUpdate(payload, noteApi);
		} else {
			delete(de.key, noteApi);
		}
	}

	private void createOrUpdate(String payload, INote noteApi) {
		ItemValue<VNote> item = mrReader.read(payload);
		ItemValue<VNote> existing = noteApi.getCompleteById(item.internalId);
		if (existing != null) {
			noteApi.updateWithItem(item);
			monitor.log("Update VNote '" + item.displayName + "'");
		} else {
			noteApi.createWithItem(item);
			monitor.log("Create VNote '" + item.displayName + "'");
		}
	}

	private void delete(RecordKey key, INote noteApi) {
		try {
			noteApi.deleteById(key.id);
		} catch (Exception e) {
			monitor.log("Failed to delete resourceTypes: " + key);
		}
	}

}
