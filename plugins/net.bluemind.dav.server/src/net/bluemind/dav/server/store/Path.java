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

import java.util.Optional;
import java.util.regex.Pattern;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.dav.server.Proxy;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.system.api.SysConfKeys;

public final class Path {
	private static final Pattern principal = Pattern.compile(Proxy.path + "/principals/__uids__/[^/]+/");
	private static final Pattern principalCalProxyRW = Pattern
			.compile(Proxy.path + "/principals/__uids__/[^/]+/calendar-proxy-(read|write)/");
	private static final Pattern calendar = Pattern.compile(Proxy.path + "/calendars/__uids__/[^/]+/");
	private static final Pattern scheduleInbox = Pattern.compile(Proxy.path + "/calendars/__uids__/[^/]+/inbox/");
	private static final Pattern scheduleOutbox = Pattern.compile(Proxy.path + "/calendars/__uids__/[^/]+/outbox/");

	private static final Pattern vstuffContainer = Pattern.compile(Proxy.path + "/calendars/__uids__/[^/]+/[^/]+/");
	private static final Pattern vstuff = Pattern.compile(Proxy.path + "/calendars/__uids__/[^/]+/[^/]+/.+\\.ics");
	private static final Pattern dropbox = Pattern.compile(Proxy.path + "/calendars/__uids__/[^/]+/dropbox/");
	private static final Pattern notificationEndpoint = Pattern
			.compile(Proxy.path + "/calendars/__uids__/[^/]+/notification/");
	private static final Pattern freebusy = Pattern.compile(Proxy.path + "/calendars/__uids__/[^/]+/freebusy");
	private static final Pattern calendarChildren = Pattern.compile(Proxy.path + "/calendars/__uids__/[^/]+/.+");

	public static boolean isPrincipal(String url) {
		return principal.matcher(url).matches();
	}

	public static boolean isPrincipalCalProxyRW(String url) {
		return principalCalProxyRW.matcher(url).matches();
	}

	public static boolean isScheduleInbox(String url) {
		return scheduleInbox.matcher(url).matches();
	}

	public static boolean isScheduleOutbox(String url) {
		return scheduleOutbox.matcher(url).matches();
	}

	public static boolean isNotificationEndpoint(String url) {
		return notificationEndpoint.matcher(url).matches();
	}

	public static boolean isDropbox(String url) {
		return dropbox.matcher(url).matches();
	}

	public static boolean isVStuff(String url) {
		return vstuff.matcher(url).matches();
	}

	public static boolean isVStuffContainer(String url) {
		return vstuffContainer.matcher(url).matches();
	}

	public static boolean isFreeBusy(String url) {
		return freebusy.matcher(url).matches();
	}

	public static boolean isCalendar(String url) {
		return calendar.matcher(url).matches();
	}

	public static boolean isCalendarChildren(String url) {
		return calendarChildren.matcher(url).matches();
	}

	public static String getExtUrl() {
		String externalUrl = Optional
				.ofNullable(MQ.<String, String>sharedMap("system.configuration").get(SysConfKeys.external_url.name()))
				.orElseThrow(() -> new ServerFault("External URL missing"));

		return externalUrl.trim();
	}
}
