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

import net.bluemind.calendar.api.CalendarView;
import net.bluemind.calendar.api.ICalendarViewUids;
import net.bluemind.calendar.api.internal.IInCoreCalendarView;
import net.bluemind.core.backup.continuous.RecordKey;
import net.bluemind.core.backup.continuous.dto.VersionnedItem;
import net.bluemind.core.backup.continuous.restore.domains.RestoreLogger;
import net.bluemind.core.container.api.IRestoreItemCrudSupport;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.core.utils.JsonUtils.ValueReader;
import net.bluemind.domain.api.Domain;

public class RestoreCalendarView extends CrudItemRestore<CalendarView> {

	private static final ValueReader<VersionnedItem<CalendarView>> reader = JsonUtils
			.reader(new TypeReference<VersionnedItem<CalendarView>>() {
			});
	private final IServiceProvider target;

	Set<String> validatedBooks = ConcurrentHashMap.newKeySet();

	public RestoreCalendarView(RestoreLogger log, ItemValue<Domain> domain, IServiceProvider target) {
		super(log, domain);
		this.target = target;
	}

	@Override
	public String type() {
		return ICalendarViewUids.TYPE;
	}

	@Override
	protected ValueReader<VersionnedItem<CalendarView>> reader() {
		return reader;
	}

	@Override
	protected IInCoreCalendarView api(ItemValue<Domain> domain, RecordKey key) {
		return target.instance(IInCoreCalendarView.class, key.uid); // domain.uid,
	}

	@Override
	protected void delete(IRestoreItemCrudSupport<CalendarView> api, RecordKey key, String uid) {
		((IInCoreCalendarView) api).delete(uid, true);
	}

}
