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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.calendar.api.CalendarsVEventQuery;
import net.bluemind.calendar.api.ICalendar;
import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.ICalendars;
import net.bluemind.calendar.api.VEventQuery;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.ContainerQuery;
import net.bluemind.core.container.api.ContainerSubscriptionDescriptor;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ItemContainerValue;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.user.api.IUserSubscription;

public class CalendarsService implements ICalendars {

	private static final Logger logger = LoggerFactory.getLogger(CalendarsService.class);

	private BmContext context;

	public CalendarsService(BmContext context) {
		this.context = context;
	}

	@Override
	public List<ItemContainerValue<VEventSeries>> search(CalendarsVEventQuery query) throws ServerFault {
		List<ItemContainerValue<VEventSeries>> ret = new ArrayList<>();

		Set<String> containers = null == query.containers ? new HashSet<>() : new HashSet<>(query.containers);

		if (null != query.owner) {
			ContainerQuery containerQuery = ContainerQuery.ownerAndType(query.owner, "calendar");
			final IContainers containerService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
					.instance(IContainers.class);
			containerService.allForUser(context.getSecurityContext().getContainerUid(), query.owner, containerQuery)
					.stream().filter(cal -> {
						return cal.verbs.stream().anyMatch(v -> v.can(Verb.Read));
					}).forEach(cal -> containers.add(cal.uid));
		}

		if (null == query.owner && containers.isEmpty()) {

			List<ContainerSubscriptionDescriptor> subscriptions = context.getServiceProvider()
					.instance(IUserSubscription.class, context.getSecurityContext().getContainerUid())
					.listSubscriptions(context.getSecurityContext().getSubject(), ICalendarUids.TYPE);

			for (ContainerSubscriptionDescriptor c : subscriptions) {
				containers.add(c.containerUid);
			}

		}

		for (String containerUid : containers) {

			try {

				ICalendar cal = context.provider().instance(ICalendar.class, containerUid);

				VEventQuery eventQuery = query.eventQuery;
				ListResult<ItemValue<VEventSeries>> resp = cal.search(eventQuery);
				for (ItemValue<VEventSeries> vevent : resp.values) {
					ItemContainerValue<VEventSeries> v = ItemContainerValue.create(containerUid, vevent, vevent.value);
					ret.add(v);
				}
			} catch (ServerFault e) {
				if (e.getCode() == ErrorCode.PERMISSION_DENIED) {
					logger.warn("user {} try to access {} but doesnt have right",
							context.getSecurityContext().getSubject() + "@"
									+ context.getSecurityContext().getContainerUid(),
							containerUid);
				} else {
					throw e;
				}
			}
		}

		return ret;
	}

}
