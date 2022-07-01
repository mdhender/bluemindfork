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
package net.bluemind.calendar.service.internal;

import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.calendar.api.CalendarView;
import net.bluemind.calendar.api.CalendarViewChanges;
import net.bluemind.calendar.api.IUserCalendarViews;
import net.bluemind.calendar.api.internal.IInCoreCalendarView;
import net.bluemind.calendar.persistence.CalendarViewStore;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerChangelog;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.ItemChangelog;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.ItemVersion;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.persistence.DataSourceRouter;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.rest.BmContext;

public class CalendarViewService implements IInCoreCalendarView, IUserCalendarViews {

	private static final Logger logger = LoggerFactory.getLogger(CalendarViewService.class);

	private Container container;
	private ContainerCalendarViewStoreService storeService;
	private CalendarViewStore store;
	private CalendarViewSanitizer sanitizer;
	private RBACManager rbacManager;

	public CalendarViewService(BmContext context, Container container) {
		DataSource ds = DataSourceRouter.get(context, container.uid);
		this.container = container;
		store = new CalendarViewStore(ds, container);
		storeService = new ContainerCalendarViewStoreService(ds, context.getSecurityContext(), container,
				"calendarview", store);
		sanitizer = new CalendarViewSanitizer();
		rbacManager = RBACManager.forContext(context).forContainer(container);
	}

	@Override
	public ItemValue<CalendarView> getComplete(String uid) throws ServerFault {
		rbacManager.check(Verb.Read.name());
		return storeService.get(uid, null);
	}

	@Override
	public void create(String uid, CalendarView view) throws ServerFault {
		ItemValue<CalendarView> item = ItemValue.create(uid, view);
		item.displayName = getSummary(view);
		create(item);
	}

	private void create(ItemValue<CalendarView> item) throws ServerFault {
		rbacManager.check(Verb.Write.name());
		CalendarView view = item.value;
		sanitizer.sanitize(view);

		logger.info("Create view {}", view.label);
		storeService.create(item.item(), item.value);
	}

	@Override
	public void update(String uid, CalendarView view) throws ServerFault {
		ItemValue<CalendarView> item = ItemValue.create(uid, view);
		update(item);
	}

	private void update(ItemValue<CalendarView> item) throws ServerFault {
		rbacManager.check(Verb.Write.name());
		String uid = item.uid;
		CalendarView view = item.value;
		sanitizer.sanitize(view);

		ItemValue<CalendarView> old = getComplete(uid);
		if (old == null) {
			throw ServerFault.notFound("entry[" + uid + "]@" + container.uid + " not found");
		}

		logger.info("Update view {}", view.label);
		storeService.update(item.item(), getSummary(view), item.value);
	}

	@Override
	public void delete(String uid) throws ServerFault {
		delete(uid, false);
	}

	@Override
	public void delete(String uid, boolean force) throws ServerFault {
		rbacManager.check(Verb.Write.name());
		ItemValue<CalendarView> old = getComplete(uid);
		if (old == null) {
			throw ServerFault.notFound("entry[" + uid + "]@" + container.uid + " not found");
		}
		if (!force && old.value.isDefault) {
			throw new ServerFault("entry[" + uid + "]@" + container.uid + " is the default view and cannot be deleted ",
					ErrorCode.PERMISSION_DENIED);
		}
		storeService.delete(uid);
	}

	@Override
	public ListResult<ItemValue<CalendarView>> list() throws ServerFault {
		rbacManager.check(Verb.Read.name());

		ListResult<ItemValue<CalendarView>> ret = new ListResult<>();
		List<String> allUids = storeService.allUids();
		List<ItemValue<CalendarView>> values = new ArrayList<>();

		for (String uid : allUids) {
			ItemValue<CalendarView> val = storeService.get(uid, null);
			if (val.value != null) {
				values.add(val);
			}
		}
		ret.total = values.size();
		ret.values = values;

		return ret;
	}

	/**
	 * @param view
	 * @return
	 */
	private String getSummary(CalendarView view) {
		return view.label;
	}

	@Override
	public ContainerChangeset<String> changeset(Long since) throws ServerFault {
		rbacManager.check(Verb.Read.name());
		if (since == null) {
			since = 0L;
		}
		return storeService.changeset(since, Long.MAX_VALUE);
	}

	@Override
	public List<ItemValue<CalendarView>> multipleGet(List<String> uids) throws ServerFault {
		rbacManager.check(Verb.Read.name());
		return storeService.getMultiple(uids);
	}

	@Override
	public void updates(CalendarViewChanges changes) throws ServerFault {
		rbacManager.check(Verb.Write.name());

		if (changes != null) {

			if (changes.add != null) {
				for (CalendarViewChanges.ItemAdd item : changes.add) {
					try {
						create(item.uid, item.value);
					} catch (ServerFault sf) {
						if (sf.getCode() == ErrorCode.ALREADY_EXISTS) {
							logger.warn("CalendarView uid {} was sent as created but already exists. We update it",
									item.uid);
							update(item.uid, item.value);
						}
					}

				}
			}

			if (changes.modify != null) {
				for (CalendarViewChanges.ItemModify item : changes.modify) {
					try {
						update(item.uid, item.value);
					} catch (ServerFault sf) {
						if (sf.getCode() == ErrorCode.NOT_FOUND) {
							logger.warn("CalendarView uid {} was sent as updated but does not exist. We create it",
									item.uid);
							create(item.uid, item.value);
						}
					}
				}
			}

			if (changes.delete != null) {
				for (CalendarViewChanges.ItemDelete item : changes.delete) {
					try {
						delete(item.uid);
					} catch (ServerFault sf) {
						if (sf.getCode() == ErrorCode.NOT_FOUND) {
							logger.warn("CalendarView uid {} was sent as deleted but does not exist.", item.uid);
						}
					}

				}
			}

		}

	}

	@Override
	public void setDefault(String id) throws ServerFault {
		rbacManager.check(Verb.Write.name());
		storeService.setDefault(id);
	}

	@Override
	public ItemValue<CalendarView> getCompleteById(long id) {
		rbacManager.check(Verb.Read.name());
		return storeService.get(id, null);
	}

	@Override
	public List<ItemValue<CalendarView>> multipleGetById(List<Long> ids) {
		rbacManager.check(Verb.Read.name());
		return storeService.getMultipleById(ids);
	}

	@Override
	public ItemChangelog itemChangelog(String itemUid, Long since) throws ServerFault {
		rbacManager.check(Verb.Read.name());
		return storeService.changelog(itemUid, since, Long.MAX_VALUE);
	}

	@Override
	public ContainerChangelog containerChangelog(Long since) throws ServerFault {
		rbacManager.check(Verb.Read.name());
		return storeService.changelog(since, Long.MAX_VALUE);
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
	public CalendarView get(String uid) throws ServerFault {
		ItemValue<CalendarView> item = getComplete(uid);
		return item != null ? item.value : null;
	}

	@Override
	public void restore(ItemValue<CalendarView> item, boolean isCreate) {
		if (isCreate) {
			create(item);
		} else {
			update(item);
		}

	}
}
