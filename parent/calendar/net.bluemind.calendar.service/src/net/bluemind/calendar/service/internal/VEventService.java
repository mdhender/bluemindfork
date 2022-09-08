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
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import io.vertx.core.buffer.Buffer;
import net.bluemind.calendar.api.IVEvent;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.calendar.api.internal.IInternalCalendar;
import net.bluemind.calendar.helper.ical4j.VEventServiceHelper;
import net.bluemind.core.api.Stream;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.rest.base.GenericStream;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.ITasksManager;
import net.bluemind.directory.api.BaseDirEntry;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.icalendar.parser.CalendarOwner;
import net.bluemind.icalendar.parser.Sudo;
import net.bluemind.tag.api.ITagUids;
import net.bluemind.tag.api.ITags;
import net.bluemind.tag.api.TagRef;

public class VEventService implements IVEvent {

	private static Logger logger = LoggerFactory.getLogger(VEventService.class);

	private IInternalCalendar calendarService;

	private BmContext context;

	private RBACManager rbacManager;

	private Container container;

	public VEventService(BmContext context, Container container) throws ServerFault {
		this.context = context;
		this.container = container;
		this.calendarService = context.provider().instance(IInternalCalendar.class, container.uid);
		rbacManager = RBACManager.forContext(context).forContainer(container);
	}

	@Override
	public String exportIcs(String uid) throws ServerFault {
		// acl checked in calendarService

		ItemValue<VEventSeries> vevent = calendarService.getComplete(uid);

		if (vevent == null) {
			logger.warn("Event uid {} not found", uid);
			return null;
		}

		return VEventServiceHelper.convertToIcs(vevent);
	}

	@Override
	public TaskRef importIcs(Stream ics) throws ServerFault {
		rbacManager.check(Verb.Write.name());

		List<TagRef> allTags = new ArrayList<>();

		BaseDirEntry.Kind calOwnerType = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDirectory.class, container.domainUid).findByEntryUid(container.owner).kind;

		if (calOwnerType != Kind.CALENDAR && calOwnerType != Kind.RESOURCE) {
			// owner tags
			allTags.addAll(getTagsService().all().stream()
					.map(tag -> TagRef.create(ITagUids.defaultUserTags(container.owner), tag))
					.collect(Collectors.toList()));
		}

		// domain tags
		ITags service = context.provider().instance(ITags.class, ITagUids.defaultUserTags(container.domainUid));
		allTags.addAll(
				service.all().stream().map(tag -> TagRef.create(ITagUids.defaultUserTags(container.domainUid), tag))
						.collect(Collectors.toList()));

		MultipleCalendarICSImport multipleCalendarICSImport = new MultipleCalendarICSImport(calendarService, ics,
				Optional.of(new CalendarOwner(container.domainUid, container.owner, calOwnerType)), allTags,
				ICSImportTask.Mode.IMPORT);

		return context.provider().instance(ITasksManager.class).run(multipleCalendarICSImport);
	}

	private ITags getTagsService() {
		if (container.owner.equals(context.getSecurityContext().getSubject())) {
			return context.getServiceProvider().instance(ITags.class, ITagUids.defaultUserTags(container.owner));
		} else {
			try (Sudo asUser = new Sudo(container.owner, container.domainUid)) {
				return ServerSideServiceProvider.getProvider(asUser.context).instance(ITags.class,
						ITagUids.defaultUserTags(container.owner));
			}
		}
	}

	@Override
	public Stream exportAll() throws ServerFault {
		// acl checked in calendarService
		List<String> allUids = calendarService.all();

		if (allUids.isEmpty()) {
			return emptyCalendar();
		}

		List<List<String>> partitioned = Lists.partition(allUids, 30);
		AtomicInteger index = new AtomicInteger(0);
		GenericStream<String> stream = new GenericStream<String>() {

			@Override
			protected StreamState<String> next() throws Exception {
				if (index.get() == partitioned.size()) {
					return StreamState.end();
				}
				List<String> uids = partitioned.get(index.get());
				List<ItemValue<VEventSeries>> events = calendarService.multipleGet(uids);
				String ics = VEventServiceHelper.convertToIcs(events);
				if (index.get() != 0) {
					ics = IcsUtil.stripHeader(ics);
				}
				if (index.get() < partitioned.size() - 1) {
					ics = IcsUtil.stripFooter(ics);
				}
				index.incrementAndGet();
				return StreamState.data(ics);

			}

			@Override
			protected Buffer serialize(String n) throws Exception {
				return Buffer.buffer(n.getBytes());
			}
		};

		return VertxStream.stream(stream);
	}

	private Stream emptyCalendar() {
		String cal = "BEGIN:VCALENDAR\r\n" + "PRODID:-//BlueMind//BlueMind Calendar//FR\r\n" + "VERSION:2.0\r\n"
				+ "CALSCALE:GREGORIAN\r\n" + "END:VCALENDAR\r\n";
		return GenericStream.simpleValue(cal, (s) -> s.getBytes());
	}

}
