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
package net.bluemind.dav.server.proto.props.caldav;

import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

import net.bluemind.dav.server.Proxy;
import net.bluemind.dav.server.proto.IPropertyValue;
import net.bluemind.dav.server.proto.NS;
import net.bluemind.dav.server.proto.props.HrefSet;
import net.bluemind.dav.server.proto.props.IPropertyFactory;
import net.bluemind.dav.server.store.DavResource;
import net.bluemind.dav.server.store.LoggedCore;
import net.bluemind.dav.server.store.ResType;

public class CalendarFreeBusySet extends HrefSet {
	public static final QName NAME = new QName(NS.CALDAV, "calendar-free-busy-set");

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(CalendarFreeBusySet.class);

	public CalendarFreeBusySet() {
		super(NAME);
	}

	public static IPropertyFactory factory() {
		return new IPropertyFactory() {
			@Override
			public IPropertyValue create() {
				return new CalendarFreeBusySet();
			}
		};
	}

	@Override
	public void fetch(LoggedCore lc, DavResource dr) throws Exception {
		ResType rt = dr.getResType();
		if (rt == ResType.SCHEDULE_INBOX) {
			hrefs = ImmutableList.of(Proxy.path + "/calendars/__uids__/" + dr.getUid() + "/calendar/");
		}
	}

}
