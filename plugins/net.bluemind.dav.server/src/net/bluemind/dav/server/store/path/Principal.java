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

import net.bluemind.dav.server.proto.props.caldav.CalendarHomeSet;
import net.bluemind.dav.server.proto.props.caldav.CalendarUserAddressSet;
import net.bluemind.dav.server.proto.props.caldav.ScheduleInboxUrl;
import net.bluemind.dav.server.proto.props.caldav.ScheduleOutboxUrl;
import net.bluemind.dav.server.proto.props.calendarserver.CalendarProxyReadFor;
import net.bluemind.dav.server.proto.props.calendarserver.CalendarProxyWriteFor;
import net.bluemind.dav.server.proto.props.calendarserver.EmailAddressSet;
import net.bluemind.dav.server.proto.props.calendarserver.NotificationUrl;
import net.bluemind.dav.server.proto.props.carddav.AddressbookHomeSet;
import net.bluemind.dav.server.proto.props.webdav.CurrentUserPrincipal;
import net.bluemind.dav.server.proto.props.webdav.DisplayName;
import net.bluemind.dav.server.proto.props.webdav.PrincipalCollectionSet;
import net.bluemind.dav.server.proto.props.webdav.PrincipalUrl;
import net.bluemind.dav.server.proto.props.webdav.ResourceId;
import net.bluemind.dav.server.proto.props.webdav.SupportedReportSet;
import net.bluemind.dav.server.store.DavResource;
import net.bluemind.dav.server.store.ResType;

public class Principal extends DavResource {

	private static final Set<QName> properties;
	private static final List<QName> types;

	static {
		Set<QName> props = new HashSet<>();
		props.add(PrincipalUrl.NAME);
		props.add(CurrentUserPrincipal.NAME);
		props.add(DisplayName.NAME);
		props.add(PrincipalCollectionSet.NAME);
		props.add(SupportedReportSet.NAME);
		props.add(ResourceId.NAME);

		props.add(CalendarHomeSet.NAME);
		props.add(AddressbookHomeSet.NAME);
		props.add(CalendarUserAddressSet.NAME);
		props.add(ScheduleInboxUrl.NAME);
		props.add(ScheduleOutboxUrl.NAME);

		// disable attachments
		// props.add(DropboxHomeUrl.NAME);
		props.add(NotificationUrl.NAME);
		props.add(EmailAddressSet.NAME);

		props.add(CalendarProxyReadFor.NAME);
		props.add(CalendarProxyWriteFor.NAME);

		properties = ImmutableSet.copyOf(props);

		types = ImmutableList.of();
	}

	public Principal(String path) {
		super(path, ResType.PRINCIPAL);
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
