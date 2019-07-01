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
package net.bluemind.dav.server.store;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;

import net.bluemind.addressbook.api.IAddressBook;
import net.bluemind.calendar.api.ICalendar;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.ContainerSubscriptionDescriptor;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.dav.server.DavActivator;
import net.bluemind.dav.server.proto.Depth;
import net.bluemind.dav.server.proto.IPropertyValue;
import net.bluemind.dav.server.proto.props.IPropertyFactory;
import net.bluemind.dav.server.proto.props.appleical.Autoprovisioned;
import net.bluemind.dav.server.proto.props.appleical.CalendarColor;
import net.bluemind.dav.server.proto.props.appleical.CalendarOrder;
import net.bluemind.dav.server.proto.props.appleical.LanguageCode;
import net.bluemind.dav.server.proto.props.appleical.LocationCode;
import net.bluemind.dav.server.proto.props.appleical.RefreshRate;
import net.bluemind.dav.server.proto.props.caldav.CalendarAlarm;
import net.bluemind.dav.server.proto.props.caldav.CalendarDescription;
import net.bluemind.dav.server.proto.props.caldav.CalendarFreeBusySet;
import net.bluemind.dav.server.proto.props.caldav.CalendarHomeSet;
import net.bluemind.dav.server.proto.props.caldav.CalendarTimezone;
import net.bluemind.dav.server.proto.props.caldav.CalendarUserAddressSet;
import net.bluemind.dav.server.proto.props.caldav.DefaultAlarmVEventDate;
import net.bluemind.dav.server.proto.props.caldav.DefaultAlarmVEventDateTime;
import net.bluemind.dav.server.proto.props.caldav.ScheduleCalendarTransp;
import net.bluemind.dav.server.proto.props.caldav.ScheduleDefaultCalendarUrl;
import net.bluemind.dav.server.proto.props.caldav.ScheduleInboxUrl;
import net.bluemind.dav.server.proto.props.caldav.ScheduleOutboxUrl;
import net.bluemind.dav.server.proto.props.caldav.ScheduleTag;
import net.bluemind.dav.server.proto.props.caldav.SupportedCalendarComponentSet;
import net.bluemind.dav.server.proto.props.caldav.SupportedCalendarComponentSets;
import net.bluemind.dav.server.proto.props.calendarserver.AllowedCalendarComponentSet;
import net.bluemind.dav.server.proto.props.calendarserver.AllowedSharingModes;
import net.bluemind.dav.server.proto.props.calendarserver.CalendarAvailability;
import net.bluemind.dav.server.proto.props.calendarserver.CalendarProxyReadFor;
import net.bluemind.dav.server.proto.props.calendarserver.CalendarProxyWriteFor;
import net.bluemind.dav.server.proto.props.calendarserver.ChecksumVersions;
import net.bluemind.dav.server.proto.props.calendarserver.DropboxHomeUrl;
import net.bluemind.dav.server.proto.props.calendarserver.EmailAddressSet;
import net.bluemind.dav.server.proto.props.calendarserver.GetCTag;
import net.bluemind.dav.server.proto.props.calendarserver.Invite;
import net.bluemind.dav.server.proto.props.calendarserver.MeCard;
import net.bluemind.dav.server.proto.props.calendarserver.NotificationType;
import net.bluemind.dav.server.proto.props.calendarserver.NotificationUrl;
import net.bluemind.dav.server.proto.props.calendarserver.PrePublishUrl;
import net.bluemind.dav.server.proto.props.calendarserver.PublishUrl;
import net.bluemind.dav.server.proto.props.calendarserver.PushKey;
import net.bluemind.dav.server.proto.props.calendarserver.PushTransports;
import net.bluemind.dav.server.proto.props.calendarserver.Source;
import net.bluemind.dav.server.proto.props.calendarserver.SubscribedStripAlarms;
import net.bluemind.dav.server.proto.props.calendarserver.SubscribedStripAttachments;
import net.bluemind.dav.server.proto.props.calendarserver.SubscribedStripTodos;
import net.bluemind.dav.server.proto.props.calendarserver.XmppServer;
import net.bluemind.dav.server.proto.props.calendarserver.XmppUri;
import net.bluemind.dav.server.proto.props.carddav.AddressBookDescription;
import net.bluemind.dav.server.proto.props.carddav.AddressbookHomeSet;
import net.bluemind.dav.server.proto.props.carddav.DirectoryGateway;
import net.bluemind.dav.server.proto.props.carddav.MaxImageSize;
import net.bluemind.dav.server.proto.props.carddav.MaxResourceSize;
import net.bluemind.dav.server.proto.props.carddav.PrincipalAddress;
import net.bluemind.dav.server.proto.props.mecom.BulkRequests;
import net.bluemind.dav.server.proto.props.webdav.AddMember;
import net.bluemind.dav.server.proto.props.webdav.CurrentUserPrincipal;
import net.bluemind.dav.server.proto.props.webdav.CurrentUserPrivilegeSet;
import net.bluemind.dav.server.proto.props.webdav.DisplayName;
import net.bluemind.dav.server.proto.props.webdav.GetContentType;
import net.bluemind.dav.server.proto.props.webdav.GetETag;
import net.bluemind.dav.server.proto.props.webdav.GroupMemberSet;
import net.bluemind.dav.server.proto.props.webdav.GroupMembership;
import net.bluemind.dav.server.proto.props.webdav.Owner;
import net.bluemind.dav.server.proto.props.webdav.PrincipalCollectionSet;
import net.bluemind.dav.server.proto.props.webdav.PrincipalUrl;
import net.bluemind.dav.server.proto.props.webdav.QuotaAvailableBytes;
import net.bluemind.dav.server.proto.props.webdav.QuotaUsedBytes;
import net.bluemind.dav.server.proto.props.webdav.ResourceId;
import net.bluemind.dav.server.proto.props.webdav.ResourceType;
import net.bluemind.dav.server.proto.props.webdav.SupportedReportSet;
import net.bluemind.dav.server.proto.props.webdav.SyncToken;
import net.bluemind.todolist.api.ITodoList;
import net.bluemind.user.api.IUserSubscription;

public final class DavStore {

	private static final Logger logger = LoggerFactory.getLogger(DavStore.class);
	private static final ConcurrentMap<QName, IPropertyFactory> factories = new ConcurrentHashMap<>();

	static {
		init();
	}

	private static void init() {
		// dav
		reg(AddMember.factory());
		reg(CurrentUserPrincipal.factory());
		reg(CurrentUserPrivilegeSet.factory());
		reg(DisplayName.factory());
		reg(GetContentType.factory());
		reg(GetETag.factory());
		reg(GroupMemberSet.factory());
		reg(GroupMembership.factory());
		reg(Owner.factory());
		reg(PrincipalCollectionSet.factory());
		reg(PrincipalUrl.factory());
		reg(QuotaAvailableBytes.factory());
		reg(QuotaUsedBytes.factory());
		reg(ResourceId.factory());
		reg(ResourceType.factory());
		reg(SupportedReportSet.factory());
		reg(SyncToken.factory());

		// caldav
		reg(CalendarDescription.factory());
		reg(CalendarFreeBusySet.factory());
		reg(CalendarHomeSet.factory());
		reg(CalendarTimezone.factory());
		reg(CalendarUserAddressSet.factory());
		reg(DefaultAlarmVEventDate.factory());
		reg(DefaultAlarmVEventDateTime.factory());
		reg(ScheduleCalendarTransp.factory());
		reg(ScheduleDefaultCalendarUrl.factory());
		reg(ScheduleInboxUrl.factory());
		reg(ScheduleOutboxUrl.factory());
		reg(SupportedCalendarComponentSet.factory());
		reg(SupportedCalendarComponentSets.factory());
		reg(ScheduleTag.factory());

		// calendarserver.org
		reg(AllowedCalendarComponentSet.factory());
		reg(AllowedSharingModes.factory());
		reg(CalendarAvailability.factory());
		reg(CalendarProxyReadFor.factory());
		reg(CalendarProxyWriteFor.factory());
		reg(ChecksumVersions.factory());
		reg(DropboxHomeUrl.factory());
		reg(EmailAddressSet.factory());
		reg(GetCTag.factory());
		reg(Invite.factory());
		reg(MeCard.factory());
		reg(NotificationType.factory());
		reg(NotificationUrl.factory());
		reg(PrePublishUrl.factory());
		reg(PublishUrl.factory());
		reg(PushKey.factory());
		reg(PushTransports.factory());
		reg(Source.factory());
		reg(SubscribedStripAlarms.factory());
		reg(SubscribedStripAttachments.factory());
		reg(SubscribedStripTodos.factory());
		reg(XmppServer.factory());
		reg(XmppUri.factory());

		// me.com
		reg(BulkRequests.factory());

		// apple ical
		reg(Autoprovisioned.factory());
		reg(CalendarAlarm.factory());
		reg(CalendarColor.factory());
		reg(CalendarOrder.factory());
		reg(LanguageCode.factory());
		reg(LocationCode.factory());
		reg(RefreshRate.factory());

		// carddav
		reg(AddressbookHomeSet.factory());
		reg(PrincipalAddress.factory());
		reg(DirectoryGateway.factory());
		reg(MaxImageSize.factory());
		reg(MaxResourceSize.factory());
		reg(AddressBookDescription.factory());

	}

	private static void reg(IPropertyFactory factory) {
		IPropertyValue pv = factory.create();
		factories.put(pv.getName(), factory);
	}

	private final LoggedCore lc;

	public DavStore(LoggedCore lc) {
		this.lc = lc;
	}

	public IPropertyValue getValue(QName prop, DavResource dr) {
		IPropertyFactory facto = factories.get(prop);
		if (facto == null) {
			RuntimeException rte = new RuntimeException("Unknown property " + prop);
			if (DavActivator.devMode) {
				logger.error(rte.getMessage(), rte);
				System.exit(1);
			} else {
				throw rte;
			}
		}
		if (dr.hasProperty(prop)) {
			IPropertyValue pv = facto.create();
			try {
				pv.fetch(lc, dr);
			} catch (Exception e) {
				Throwables.propagate(e);
			}
			return pv;
		} else {
			return null;
		}
	}

	public PropSetResult setValue(QName prop, Element value, DavResource dr) {
		IPropertyFactory facto = factories.get(prop);
		if (facto == null) {
			RuntimeException rte = new RuntimeException("Unknown property " + prop);
			if (DavActivator.devMode) {
				logger.error(rte.getMessage(), rte);
				System.exit(1);
			} else {
				throw rte;
			}
		}
		IPropertyValue pv = facto.create();
		logger.info("Should update value of {} on {} using {}", prop, dr.getPath(), pv);
		try {
			pv.set(lc, dr, value);
			return PropSetResult.UPDATED;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return PropSetResult.DENIED;
		}
	}

	public IPropertyValue getValue(Property prop, DavResource dr) {
		IPropertyFactory facto = factories.get(prop.getQName());
		if (facto == null) {
			RuntimeException rte = new RuntimeException("Unknown property " + prop.getQName());
			if (DavActivator.devMode) {
				logger.error(rte.getMessage(), rte);
				System.exit(1);
			} else {
				throw rte;
			}
		}
		if (dr.hasProperty(prop.getQName())) {
			IPropertyValue pv = facto.create();
			try {
				pv.expand(lc, dr, prop.getChildren());
			} catch (Exception e) {
				Throwables.propagate(e);
			}
			return pv;
		} else {
			return null;
		}
	}

	public DavResource from(String path) {
		DavResource ret = null;
		// test for those as they match vstuffContainer too
		if (ResType.NOTIFICATIONS.matcher(path).matches()) {
			ret = ResType.NOTIFICATIONS.from(path, lc);
		} else if (ResType.SCHEDULE_INBOX.matcher(path).matches()) {
			ret = ResType.SCHEDULE_INBOX.from(path, lc);
		} else if (ResType.SCHEDULE_OUTBOX.matcher(path).matches()) {
			ret = ResType.SCHEDULE_OUTBOX.from(path, lc);
		} else {
			for (ResType type : ResType.values()) {
				Matcher matcher = type.matcher(path);
				if (matcher.matches()) {
					ret = type.from(path, lc);
					break;
				} else {
					// logger.warn("{} does not match '{}'", type, path);
				}
			}
		}
		if (ret == null) {
			logger.error("Dav resource at '{}' unknown", path);
			if (DavActivator.devMode) {
				System.exit(1);
			}
		} else {
			logger.info("[{}] {}", ret.getResType(), path);
		}
		return ret;
	}

	public List<DavResource> addChildren(DavResource dr, Depth depth) {
		logger.info("Add children {} at depth {}", dr, depth);
		List<DavResource> ret = Lists.newLinkedList();
		ResType rt = dr.getResType();
		switch (depth) {
		case INFINITY_NOROOT:
			logger.error("Infinity noroot request for " + dr);
			break;
		case ONE:
			ret.add(dr);
			if (rt == ResType.CALENDAR) {
				addCalChildren(ret, dr);
			} else if (rt == ResType.ADDRESSBOOK) {
				addBookChildren(ret, dr);
			} else if (rt == ResType.VSTUFF_CONTAINER) {
				addEvents(ret, dr);
			} else if (rt == ResType.VCARDS_CONTAINER) {
				addContactsAndLists(ret, dr);
			}
			break;
		case ONE_NOROOT:
			if (rt == ResType.CALENDAR) {
				addCalChildren(ret, dr);
			} else if (rt == ResType.ADDRESSBOOK) {
				addBookChildren(ret, dr);
			} else if (rt == ResType.VSTUFF_CONTAINER) {
				addEvents(ret, dr);
			} else if (rt == ResType.VCARDS_CONTAINER) {
				addContactsAndLists(ret, dr);
			}
			break;
		case INFINITY:
			logger.warn("Infinity depth request for " + dr);
		case ZERO:
			ret.add(dr);
			break;
		}

		return ret;
	}

	private void addContactsAndLists(List<DavResource> ret, DavResource dr) {
		ContainerDescriptor book = BookUtils.addressbook(lc, dr);
		if (book == null) {
			logger.warn("No addressbook found for '{}'", dr.getPath());
			return;
		}
		try {
			IAddressBook bookApi = lc.getCore().instance(IAddressBook.class, book.uid);
			List<String> uids = bookApi.allUids();
			String path = dr.getPath();
			int resetLen = path.length();
			StringBuilder sb = new StringBuilder(resetLen + 128);
			sb.append(path);
			for (String uid : uids) {
				sb.setLength(resetLen);
				sb.append(URLEncoder.encode(uid, "utf-8")).append(".vcf");
				ret.add(from(sb.toString()));
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

	}

	/**
	 * @param ret
	 * @param path the vevents container path
	 */
	private void addEvents(List<DavResource> ret, DavResource dr) {
		ContainerDescriptor cd = lc.vStuffContainer(dr);
		String containerUid = cd.uid;
		String path = dr.getPath();
		int beforeSize = ret.size();
		try {
			long time = System.currentTimeMillis();
			if ("todolist".equals(cd.type)) {
				ITodoList calApi = lc.getCore().instance(ITodoList.class, containerUid);
				ContainerChangeset<String> log = calApi.changeset(0L);
				ret.addAll(log.created.stream().map(uid -> from(path + uid + ".ics")).collect(Collectors.toList()));
			} else {
				ICalendar calApi = lc.getCore().instance(ICalendar.class, containerUid);
				ret.addAll(calApi.all().stream().map(uid -> from(path + uid + ".ics")).collect(Collectors.toList()));
			}
			time = System.currentTimeMillis() - time;
			logger.info("[{}: {}] {} uids in {}ms.", cd.type, containerUid, ret.size() - beforeSize, time);
		} catch (Exception e) {
			logger.error("Fail to get {} uid: {} changes ", cd.type, containerUid, e);
		}
	}

	private void addCalChildren(List<DavResource> ret, DavResource dr) {
		String path = dr.getPath();

		List<DavResource> calendars = getCalendarDavResource("calendar", dr, path);
		ret.addAll(calendars);

		List<DavResource> todolists = getCalendarDavResource("todolist", dr, path);
		ret.addAll(todolists);

		// FIXME load from server

		ret.add(from(path + "notification/"));
		ret.add(from(path + "freebusy"));

		// disable attachments support
		// ret.add(from(path + "dropbox/"));

		ret.add(from(path + "inbox/"));
		ret.add(from(path + "outbox/"));
	}

	/**
	 * @param type
	 * @param dr
	 * @param path
	 * @return
	 */
	private List<DavResource> getCalendarDavResource(String type, DavResource dr, String path) {
		List<DavResource> ret = new ArrayList<DavResource>();
		try {
			IUserSubscription service = lc.getCore().instance(IUserSubscription.class, lc.getDomain());
			String owner = getUserUid(dr);
			List<ContainerSubscriptionDescriptor> subs = service.listSubscriptions(lc.getUser().uid, type);
			for (ContainerSubscriptionDescriptor cd : subs) {
				if (cd.owner.equals(owner)) {
					logger.debug("Add {}", cd.containerUid);
					ret.add(from(path + cd.containerUid + "/"));
				}
			}
		} catch (ServerFault e) {
			logger.error(e.getMessage(), e);
		}
		return ret;
	}

	/**
	 * @param dr
	 * @return
	 */
	private String getUserUid(DavResource dr) {
		Matcher m = dr.getResType().matcher(dr.getPath());
		m.find();
		return m.group(1);
	}

	private void addBookChildren(List<DavResource> ret, DavResource dr) {
		logger.info("Add book children, books: {}", lc.getBooks().values());
		String path = dr.getPath();
		for (ContainerDescriptor f : lc.getBooks().values()) {
			ret.add(from(path + f.uid + "/"));
		}
	}

	public boolean existingResource(DavResource dr) {
		switch (dr.getResType()) {
		// case VCARD:
		// ContainerDescriptor bookDesc = BookUtils.addressbook(lc, dr);
		// if (bookDesc == null) {
		// return false;
		// }
		// Matcher m = dr.getResType().matcher(dr.getPath());
		// m.find();
		// String vcardUid = BookUtils.itemUid(m.group(3));
		// try {
		// IAddressBook abApi = lc.getCore().instance(IAddressBook.class,
		// bookDesc.uid);
		// return abApi.getComplete(vcardUid) != null;
		// } catch (Exception e) {
		// logger.warn("{} does not exist ({})", dr.path, e.getMessage());
		// return false;
		// }
		case VCARDS_CONTAINER:
			ContainerDescriptor bookContainer = BookUtils.addressbook(lc, dr);
			return bookContainer != null;
		case VSTUFF_CONTAINER:
			ContainerDescriptor vstuffContainer = lc.vStuffContainer(dr);
			return vstuffContainer != null;
		default:
			logger.debug("Don't know how to check if {} {} exists, assume yes.", dr.getResType(), dr.getPath());
			return true;
		}
	}

}
