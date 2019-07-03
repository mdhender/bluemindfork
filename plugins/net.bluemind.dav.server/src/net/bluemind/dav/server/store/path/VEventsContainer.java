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
package net.bluemind.dav.server.store.path;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import net.bluemind.dav.server.proto.Types;
import net.bluemind.dav.server.proto.props.appleical.CalendarColor;
import net.bluemind.dav.server.proto.props.appleical.CalendarOrder;
import net.bluemind.dav.server.proto.props.caldav.CalendarTimezone;
import net.bluemind.dav.server.proto.props.caldav.DefaultAlarmVEventDate;
import net.bluemind.dav.server.proto.props.caldav.DefaultAlarmVEventDateTime;
import net.bluemind.dav.server.proto.props.caldav.ScheduleCalendarTransp;
import net.bluemind.dav.server.proto.props.caldav.ScheduleTag;
import net.bluemind.dav.server.proto.props.caldav.SupportedCalendarComponentSet;
import net.bluemind.dav.server.proto.props.calendarserver.AllowedSharingModes;
import net.bluemind.dav.server.proto.props.calendarserver.GetCTag;
import net.bluemind.dav.server.proto.props.calendarserver.Invite;
import net.bluemind.dav.server.proto.props.calendarserver.PushKey;
import net.bluemind.dav.server.proto.props.mecom.BulkRequests;
import net.bluemind.dav.server.proto.props.webdav.AddMember;
import net.bluemind.dav.server.proto.props.webdav.CurrentUserPrivilegeSet;
import net.bluemind.dav.server.proto.props.webdav.DisplayName;
import net.bluemind.dav.server.proto.props.webdav.GetContentType;
import net.bluemind.dav.server.proto.props.webdav.GetETag;
import net.bluemind.dav.server.proto.props.webdav.Owner;
import net.bluemind.dav.server.proto.props.webdav.QuotaAvailableBytes;
import net.bluemind.dav.server.proto.props.webdav.QuotaUsedBytes;
import net.bluemind.dav.server.proto.props.webdav.ResourceId;
import net.bluemind.dav.server.proto.props.webdav.ResourceType;
import net.bluemind.dav.server.proto.props.webdav.SupportedReportSet;
import net.bluemind.dav.server.proto.props.webdav.SyncToken;
import net.bluemind.dav.server.store.DavResource;
import net.bluemind.dav.server.store.ResType;

public class VEventsContainer extends DavResource {

	private static final Set<QName> properties;
	private static final List<QName> types;

	static {
		Set<QName> props = new HashSet<>();
		props.add(CurrentUserPrivilegeSet.NAME);
		props.add(Owner.NAME);
		props.add(QuotaAvailableBytes.NAME);
		props.add(QuotaUsedBytes.NAME);
		props.add(ResourceType.NAME);
		props.add(SupportedReportSet.NAME);

		props.add(AddMember.NAME);
		props.add(AllowedSharingModes.NAME);
		props.add(BulkRequests.NAME);
		props.add(CalendarColor.NAME);
		props.add(CalendarOrder.NAME);
		props.add(CalendarTimezone.NAME);
		props.add(CurrentUserPrivilegeSet.NAME);
		props.add(DefaultAlarmVEventDate.NAME);
		props.add(DefaultAlarmVEventDateTime.NAME);
		props.add(DisplayName.NAME);
		props.add(GetContentType.NAME);
		props.add(GetCTag.NAME);
		props.add(GetETag.NAME);
		props.add(Invite.NAME);
		props.add(Owner.NAME);
		props.add(PushKey.NAME);
		props.add(ResourceId.NAME);
		props.add(ScheduleCalendarTransp.NAME);
		props.add(SupportedCalendarComponentSet.NAME);
		props.add(SyncToken.NAME);
		props.add(ScheduleTag.NAME);

		properties = ImmutableSet.copyOf(props);

		types = ImmutableList.of(Types.COL, Types.CAL);
	}

	public VEventsContainer(String path) {
		super(path, ResType.VSTUFF_CONTAINER);
	}

	public VEventsContainer(String path, ResType rt) {
		super(path, rt);
	}

	@Override
	public boolean hasProperty(QName prop) {
		return properties.contains(prop);
	}

	@Override
	public Set<QName> getDefinedProperties() {
		return properties;
	}

	@Override
	public List<QName> getTypes() {
		return types;
	}

}
