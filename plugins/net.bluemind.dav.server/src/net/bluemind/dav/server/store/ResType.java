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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.bluemind.calendar.api.VEvent;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.dav.server.Proxy;
import net.bluemind.dav.server.store.path.Addressbook;
import net.bluemind.dav.server.store.path.ApnsEndpoint;
import net.bluemind.dav.server.store.path.Calendar;
import net.bluemind.dav.server.store.path.Dropbox;
import net.bluemind.dav.server.store.path.EventDropbox;
import net.bluemind.dav.server.store.path.Freebusy;
import net.bluemind.dav.server.store.path.NotificationsEndoint;
import net.bluemind.dav.server.store.path.Principal;
import net.bluemind.dav.server.store.path.PrincipalCalendarProxy;
import net.bluemind.dav.server.store.path.PrincipalsCollection;
import net.bluemind.dav.server.store.path.Root;
import net.bluemind.dav.server.store.path.ScheduleInbox;
import net.bluemind.dav.server.store.path.ScheduleOutbox;
import net.bluemind.dav.server.store.path.VCardNode;
import net.bluemind.dav.server.store.path.VCardsContainer;
import net.bluemind.dav.server.store.path.VEventNode;
import net.bluemind.dav.server.store.path.VEventsContainer;
import net.bluemind.user.api.User;

public enum ResType {

	ROOT(Proxy.path + "/?", false, new IPathFactory() {

		@Override
		public DavResource from(ResType self, String path, LoggedCore lc) {
			return new Root(path);
		}
	}),

	APNS(Proxy.path + "/apns", false, new IPathFactory() {

		@Override
		public DavResource from(ResType self, String path, LoggedCore lc) {
			return new ApnsEndpoint(path);
		}
	}),

	PRINCIPALS_COL(Proxy.path + "/principals/", false, new IPathFactory() {

		@Override
		public DavResource from(ResType self, String path, LoggedCore lc) {
			return new PrincipalsCollection(path);
		}
	}),

	/**
	 * User principal, the first group is the {@link User} uid
	 */
	PRINCIPAL(Proxy.path + "/principals/__uids__/([^/]+)/", false, new IPathFactory() {

		@Override
		public DavResource from(ResType self, String path, LoggedCore lc) {
			return new Principal(path);
		}
	}),

	PRINCIPAL_CAL_PROXY_RW(Proxy.path + "/principals/__uids__/([^/]+)/calendar-proxy-(read|write)/", false,
			new IPathFactory() {

				@Override
				public DavResource from(ResType self, String path, LoggedCore lc) {
					return new PrincipalCalendarProxy(path);
				}
			}),

	/**
	 * Root for a {@link User} calendars. the first group is the {User} uid.
	 */
	CALENDAR(Proxy.path + "/calendars/__uids__/([^/]+)/", false, new IPathFactory() {

		@Override
		public DavResource from(ResType self, String path, LoggedCore lc) {
			return new Calendar(path);
		}
	}),

	SCHEDULE_INBOX(Proxy.path + "/calendars/__uids__/([^/]+)/inbox/", true, new IPathFactory() {

		@Override
		public DavResource from(ResType self, String path, LoggedCore lc) {
			return new ScheduleInbox(path);
		}
	}),

	SCHEDULE_OUTBOX(Proxy.path + "/calendars/__uids__/([^/]+)/outbox/", true, new IPathFactory() {

		@Override
		public DavResource from(ResType self, String path, LoggedCore lc) {
			return new ScheduleOutbox(path);
		}
	}),

	/**
	 * A {@link User} default calendar. The first group is the user's uid.
	 */
	VSTUFF_CONTAINER(Proxy.path + "/calendars/__uids__/([^/]+)/([^/]+)/", true, new IPathFactory() {

		@Override
		public DavResource from(ResType self, String path, LoggedCore lc) {
			return new VEventsContainer(path);
		}
	}),

	/**
	 * A {@link VEvent} in a user's default calendar.
	 */
	VSTUFF(Proxy.path + "/calendars/__uids__/([^/]+)/([^/]+)/(.+)\\.ics", true, new IPathFactory() {

		@Override
		public DavResource from(ResType self, String path, LoggedCore lc) {
			return new VEventNode(path);
		}
	}),

	/**
	 * Endpoint for dropping event attachments
	 */
	DROPBOX(Proxy.path + "/calendars/__uids__/([^/]+)/dropbox/", true, new IPathFactory() {

		@Override
		public DavResource from(ResType self, String path, LoggedCore lc) {
			return new Dropbox(path);
		}
	}),

	VEVENT_DROPBOX(Proxy.path + "/calendars/__uids__/([^/]+)/dropbox/([^\\.]+).dropbox/", true, new IPathFactory() {

		@Override
		public DavResource from(ResType self, String path, LoggedCore lc) {
			return new EventDropbox(path);
		}
	}),

	NOTIFICATIONS(Proxy.path + "/calendars/__uids__/([^/]+)/notification/", true, new IPathFactory() {

		@Override
		public DavResource from(ResType self, String path, LoggedCore lc) {
			return new NotificationsEndoint(path);
		}
	}),

	FREEBUSY(Proxy.path + "/calendars/__uids__/([^/]+)/freebusy", true, new IPathFactory() {

		@Override
		public DavResource from(ResType self, String path, LoggedCore lc) {
			return new Freebusy(path);
		}
	}),

	/**
	 * Root for user addressbooks. osx Contact.app only support one addressbook.
	 * First group is the {@link User} uid.
	 */
	ADDRESSBOOK(Proxy.path + "/addressbooks/__uids__/([^/]+)/", false, new IPathFactory() {

		@Override
		public DavResource from(ResType self, String path, LoggedCore lc) {

			return new Addressbook(path);
		}
	}),

	VCARDS_CONTAINER(Proxy.path + "/addressbooks/__uids__/([^/]+)/([^/]+)/", false, new IPathFactory() {

		@Override
		public DavResource from(ResType self, String path, LoggedCore lc) {
			return new VCardsContainer(path);
		}
	}),

	/**
	 * A contact or dlist vcard.
	 * 
	 * First group is the uid the {@link User} uid.
	 * 
	 * Second group is the uid of the addressbook {@link ContainerDescriptor}.
	 * 
	 * Shird group is the uid of the contact
	 */
	VCARD(Proxy.path + "/addressbooks/__uids__/([^/]+)/([^/]+)/(.+)\\.vcf", true, new IPathFactory() {

		@Override
		public DavResource from(ResType self, String path, LoggedCore lc) {
			return new VCardNode(path, self);
		}
	});

	private static interface IPathFactory {

		DavResource from(ResType self, String path, LoggedCore lc);

	}

	private final Pattern p;
	private final boolean calChild;
	private final IPathFactory pf;

	private ResType(String regexp, boolean calChild, IPathFactory pf) {
		this.p = Pattern.compile(regexp);
		this.calChild = calChild;
		this.pf = pf;
	}

	public Matcher matcher(CharSequence url) {
		return p.matcher(url);
	}

	public boolean isCalChild() {
		return calChild;
	}

	public DavResource from(String path, LoggedCore lc) {
		return pf.from(this, path, lc);
	}

}
