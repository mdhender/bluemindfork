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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.addressbook.api.IAddressBook;
import net.bluemind.addressbook.api.IAddressBookUids;
import net.bluemind.addressbook.api.VCard;
import net.bluemind.calendar.api.CalendarLookupResponse;
import net.bluemind.calendar.api.CalendarLookupResponse.Type;
import net.bluemind.calendar.api.ICalendarAutocomplete;
import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.core.api.Email;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.ContainerQuery;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.BaseContainerDescriptor;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.rest.BmContext;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.DirEntryQuery;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.group.api.Group;
import net.bluemind.group.api.IGroup;

public class CalendarAutocompleteService implements ICalendarAutocomplete {

	private final int LIMIT = 10;

	private BmContext context;
	private static final Logger logger = LoggerFactory.getLogger(CalendarAutocompleteService.class);

	public CalendarAutocompleteService(BmContext context) {
		this.context = context;
	}

	@Override
	public List<CalendarLookupResponse> calendarGroupLookup(String groupUid) throws ServerFault {

		// Get all group users. We need member item uid and email (eventually
		// display
		// name too).
		List<ItemValue<VCard>> members = getGroupMembers(groupUid);
		// Get all users default calendars.
		IContainers containers = context.provider().instance(IContainers.class);
		List<ContainerDescriptor> calendars = containers.getContainers(
				members.stream().map(m -> ICalendarUids.defaultUserCalendar(m.uid)).collect(Collectors.toList()));
		// Only select calendar with read right, and then convert to
		// CalendarLookupResponse.
		// To convert the calendar we match the user vcard with the calendar and
		// build a
		// CalendarLookupResponse with the VCard infos.
		return calendars.stream().filter(c -> c.verbs.stream().anyMatch(v -> v.equals(Verb.Read)))
				.map(c -> calendarToCalendarLookupResponse(c, members)).collect(Collectors.toList());

	}

	private CalendarLookupResponse calendarToCalendarLookupResponse(ContainerDescriptor c,
			List<ItemValue<VCard>> members) {
		Optional<ItemValue<VCard>> member = members.stream()
				.filter(m -> ICalendarUids.defaultUserCalendar(m.uid).equals(c.uid)).findFirst();
		if (member.isPresent()) {
			return CalendarLookupResponse.calendar(c.uid, member.get().value.identification.formatedName.value,
					member.get().value.defaultMail(), member.get().uid);
		} else {
			throw new ServerFault("Container " + c.uid + " is not present");
		}
	}

	private List<ItemValue<VCard>> getGroupMembers(String uid) {
		List<ItemValue<VCard>> uids = new ArrayList<ItemValue<VCard>>();
		String domainUid = context.getSecurityContext().getContainerUid();
		IAddressBook book = context.su().provider().instance(IAddressBook.class,
				IAddressBookUids.userVCards(domainUid));
		ItemValue<VCard> vcard = book.getComplete(uid);
		if (vcard != null && vcard.value.organizational.member.size() > 0) {
			List<ItemValue<VCard>> members = book.multipleGet(
					vcard.value.organizational.member.stream().map(m -> m.itemUid).collect(Collectors.toList()));
			for (ItemValue<VCard> member : members) {
				if (member.value.kind == VCard.Kind.group) {
					uids.addAll(getGroupMembers(member.uid));
				} else {
					uids.add(member);
				}
			}
		}
		return uids;
	}

	@Override
	public List<CalendarLookupResponse> calendarLookup(String pattern, Verb verb) throws ServerFault {
		List<CalendarLookupResponse> ret = new ArrayList<CalendarLookupResponse>();

		IDirectory dir = context.provider().instance(IDirectory.class, context.getSecurityContext().getContainerUid());
		IGroup groups = context.su().provider().instance(IGroup.class, context.getSecurityContext().getContainerUid());

		IContainers ic = context.provider().instance(IContainers.class);
		ContainerQuery q = new ContainerQuery();
		q.type = ICalendarUids.TYPE;
		q.verb = new ArrayList<>();
		q.verb.add(verb);
		for (Verb v : Verb.values()) {
			if (!verb.can(v)) {
				q.verb.add(v);
			}
		}
		List<BaseContainerDescriptor> matchingContainers = ic.allLight(q);
		logger.debug("matchingContainers: {}", matchingContainers);
		Map<String, BaseContainerDescriptor> containerMap = matchingContainers.stream().filter(c -> c.defaultContainer)
				.collect(Collectors.toMap(c -> c.owner, c -> c));

		DirEntryQuery dq = DirEntryQuery.filterNameOrEmail(pattern);
		dq.kindsFilter = Arrays.asList(DirEntry.Kind.USER, DirEntry.Kind.GROUP, DirEntry.Kind.CALENDAR,
				DirEntry.Kind.RESOURCE);
		ListResult<ItemValue<DirEntry>> entries = dir.search(dq);

		// default calendars
		for (ItemValue<DirEntry> entry : entries.values) {
			if (ret.size() > LIMIT) {
				break;
			}
			String groupUid = entry.value.entryUid;
			if (entry.value.kind == Kind.USER || entry.value.kind == Kind.CALENDAR
					|| entry.value.kind == Kind.RESOURCE) {
				if (containerMap.containsKey(groupUid)) {
					BaseContainerDescriptor c = containerMap.get(groupUid);
					ret.add(CalendarLookupResponse.calendar(c.uid, c.name, entry.value.email, groupUid));
				}
			} else if (entry.value.kind == Kind.GROUP) {
				int userCount = groups.getExpandedUserMembers(groupUid).size();
				ItemValue<Group> group = groups.getComplete(groupUid);
				if (!group.value.hiddenMembers && userCount > 0) {
					CalendarLookupResponse r = new CalendarLookupResponse();
					r.uid = groupUid;
					r.name = entry.value.displayName;
					r.type = Type.group;
					r.memberCount = userCount;
					Email email = group.value.defaultEmail();
					if (email != null) {
						r.email = email.address;
					}
					ret.add(r);
				}
			}
		}

		if (ret.size() < LIMIT) {
			// user created calendars
			IContainers containers = context.provider().instance(IContainers.class);
			ContainerQuery cq = new ContainerQuery();
			cq.type = ICalendarUids.TYPE;
			cq.name = pattern;
			cq.size = LIMIT - ret.size();
			List<BaseContainerDescriptor> calendars = containers.allLight(cq);

			for (BaseContainerDescriptor cd : calendars) {
				if (!cd.defaultContainer) {
					ret.add(CalendarLookupResponse.calendar(cd.uid, cd.name, null, cd.owner));
				}
			}
		}
		// reorder
		Collections.sort(ret, new Comparator<CalendarLookupResponse>() {
			@Override
			public int compare(CalendarLookupResponse o1, CalendarLookupResponse o2) {
				return o1.name.toLowerCase().toString().compareTo(o2.name.toLowerCase().toString());
			}

		});

		// limit
		if (ret.size() > LIMIT) {
			ret = ret.subList(0, LIMIT);
		}

		return ret;
	}

}
