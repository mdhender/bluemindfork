/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.cti.service.internal;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.calendar.api.IFreebusyUids;
import net.bluemind.calendar.api.IVFreebusy;
import net.bluemind.calendar.api.VFreebusy;
import net.bluemind.calendar.api.VFreebusy.Slot;
import net.bluemind.calendar.api.VFreebusy.Type;
import net.bluemind.calendar.api.VFreebusyQuery;
import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.api.date.BmDateTime.Precision;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.cti.api.IComputerTelephonyIntegration;
import net.bluemind.cti.api.Status;
import net.bluemind.cti.service.CTIDeferredAction;
import net.bluemind.deferredaction.api.DeferredAction;
import net.bluemind.deferredaction.api.IDeferredAction;
import net.bluemind.deferredaction.api.IDeferredActionContainerUids;
import net.bluemind.deferredaction.registry.IDeferredActionExecutor;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.user.api.IUserSettings;

public class DeferredActionCTIExecutor implements IDeferredActionExecutor {

	private static final Logger logger = LoggerFactory.getLogger(DeferredActionCTIExecutor.class);

	private static final int TRIGGER_INTERVAL_IN_HOURS = 48;
	private final IServiceProvider provider;

	public DeferredActionCTIExecutor() {
		provider = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
	}

	@Override
	public void execute(ZonedDateTime executionDate) {
		provider.instance(IDomains.class).all().stream().filter(this::isNotGlobalVirt)
				.forEach(executeForDomain(executionDate));
	}

	private Consumer<? super ItemValue<Domain>> executeForDomain(ZonedDateTime executionDate) {
		return domain -> {
			logger.info("Handling CTI deferred actions of domain {}", domain.uid);
			IDeferredAction deferredActionService = provider.instance(IDeferredAction.class,
					IDeferredActionContainerUids.uidForDomain(domain.uid));

			Map<String, List<ItemValue<DeferredAction>>> groupedByUser = deferredActionService
					.getByActionId(CTIDeferredAction.ACTION_ID, executionDate.toInstant().toEpochMilli()).stream() //
					.collect(Collectors.groupingBy(action -> CTIDeferredAction.userUid(action.value.reference)));

			for (Entry<String, List<ItemValue<DeferredAction>>> userActions : groupedByUser.entrySet()) {
				try {
					executeUserActions(deferredActionService,
							new User(domain.uid, userActions.getKey(), userActions.getValue()));
				} catch (Exception e) {
					logger.warn("Cannot handle CTI actions of user {}@{}", userActions.getKey(), domain.uid);
				}
			}
		};
	}

	private void executeUserActions(IDeferredAction deferredActionService, User user) {
		logger.info("Handling CTI deferred actions of {}@{}", user.userUid, user.domainUid);
		IVFreebusy freebusy = provider.instance(IVFreebusy.class, IFreebusyUids.getFreebusyContainerUid(user.userUid));

		VFreebusy usersFreeBusyInfo = getUserFreeBusy(freebusy, ZonedDateTime.now().minusMinutes(5));
		usersFreeBusyInfo.slots = orderSlots(usersFreeBusyInfo.slots);
		Iterator<Slot> iter = usersFreeBusyInfo.slots.iterator();

		Slot slot = findCurrentSlot(iter);
		BmDateTime nextDate = executeCtiActions(user.domainUid, user.userUid, slot);
		storeNextDeferredAction(deferredActionService, user.userUid, nextDate);
		deleteExpiredActions(deferredActionService, user.actions);
	}

	private BmDateTime executeCtiActions(String domainUid, String userUid, Slot slot) {
		BmDateTime nextDate = null;
		if (slot == null) {
			nextDate = BmDateTimeWrapper.create(ZonedDateTime.now().plusHours(TRIGGER_INTERVAL_IN_HOURS),
					Precision.DateTime);
			manageCtiActions(domainUid, userUid, Type.FREE);
		} else if (dtstartIsInFuture(slot)) {
			manageCtiActions(domainUid, userUid, Type.FREE);
			nextDate = slot.dtstart;
		} else {
			manageCtiActions(domainUid, userUid, slot.type);
			nextDate = slot.dtend;
		}
		return nextDate;
	}

	private Slot findCurrentSlot(Iterator<Slot> iter) {
		Slot slot = null;
		while (iter.hasNext()) {
			Slot currentSlot = iter.next();
			if (!dtEndIsInPast(currentSlot)) {
				slot = currentSlot;
				break;
			}
		}
		return slot;
	}

	private boolean dtstartIsInFuture(Slot slot) {
		return toZonedDate(slot.dtstart).isAfter(ZonedDateTime.now());
	}

	private boolean dtEndIsInPast(Slot slot) {
		return !toZonedDate(slot.dtend).isAfter(ZonedDateTime.now());
	}

	private boolean manageCtiActions(String domainUid, String userUid, Type slotType) {
		Map<String, String> userSettings = provider.instance(IUserSettings.class, domainUid).get(userUid);
		if (!userSettings.containsKey("cal_set_phone_presence")
				|| userSettings.get("cal_set_phone_presence").equalsIgnoreCase("false")) {
			return false;
		}
		IComputerTelephonyIntegration ctiService = provider.instance(IComputerTelephonyIntegration.class, domainUid,
				userUid);
		if (slotType != VFreebusy.Type.FREE) {
			logger.info("Switching CTI status of {}@{} to {}", userUid, domainUid,
					userSettings.get("cal_set_phone_presence"));
			if (userSettings.get("cal_set_phone_presence").equals("dnd")) {
				ctiService.setStatus("IM", Status.create(Status.Type.DoNotDisturb, "Do not disturb"));
			} else {
				String forwardTo = userSettings.get("cal_set_phone_presence");
				ctiService.forward("IM", forwardTo);
			}
		} else {
			ctiService.setStatus("IM", Status.create(Status.Type.Available, "Available"));
		}
		return true;
	}

	private VFreebusy getUserFreeBusy(IVFreebusy freebusy, ZonedDateTime from) {
		VFreebusyQuery query = VFreebusyQuery.create(BmDateTimeWrapper.create(from, Precision.DateTime),
				BmDateTimeWrapper.create(from.plusHours(TRIGGER_INTERVAL_IN_HOURS).plusMinutes(5), Precision.DateTime));
		return freebusy.get(query);
	}

	private void deleteExpiredActions(IDeferredAction service, List<ItemValue<DeferredAction>> actions) {
		List<ItemValue<DeferredAction>> userDeferredActions = actions.stream().sorted(this::dateCompare)
				.collect(Collectors.toList());
		for (ItemValue<DeferredAction> action : userDeferredActions) {
			if (toZonedDate(action.value.executionDate).isBefore(ZonedDateTime.now())) {
				deleteAction(service, action);
			}
		}
	}

	private void deleteAction(IDeferredAction deferredActionService, ItemValue<DeferredAction> action) {
		deferredActionService.delete(action.uid);
	}

	private void storeNextDeferredAction(IDeferredAction deferredActionService, String userUid, BmDateTime nextDate) {
		DeferredAction deferredAction = new DeferredAction();
		deferredAction.executionDate = new Date(BmDateTimeWrapper.toTimestamp(nextDate.iso8601, nextDate.timezone));
		deferredAction.actionId = CTIDeferredAction.ACTION_ID;
		deferredAction.reference = CTIDeferredAction.reference(userUid);
		deferredActionService.create(UUID.randomUUID().toString(), deferredAction);
	}

	private int dateCompare(ItemValue<DeferredAction> action1, ItemValue<DeferredAction> action2) {
		return action1.value.executionDate.compareTo(action2.value.executionDate);
	}

	private ZonedDateTime toZonedDate(BmDateTime date) {
		return new BmDateTimeWrapper(date).toDateTime().withZoneSameInstant(ZoneId.of("UTC"));
	}

	private ZonedDateTime toZonedDate(Date date) {
		return Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.of("UTC"));
	}

	private boolean isNotGlobalVirt(ItemValue<Domain> domain) {
		return !"global.virt".equals(domain.value.name);
	}

	private List<Slot> orderSlots(List<Slot> slots) {
		List<Slot> ordered = new ArrayList<>(slots);
		Collections.sort(ordered, (a, b) -> {
			return toZonedDate(a.dtstart).compareTo(toZonedDate(b.dtstart));
		});
		return ordered;
	}

	private class User {
		public final String domainUid;
		public final String userUid;
		public final List<ItemValue<DeferredAction>> actions;

		public User(String domainUid, String userUid, List<ItemValue<DeferredAction>> actions) {
			this.domainUid = domainUid;
			this.userUid = userUid;
			this.actions = actions;
		}
	}

}
