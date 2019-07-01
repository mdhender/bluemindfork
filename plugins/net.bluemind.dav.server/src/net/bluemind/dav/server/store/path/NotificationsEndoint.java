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
import net.bluemind.dav.server.proto.props.calendarserver.GetCTag;
import net.bluemind.dav.server.proto.props.calendarserver.NotificationType;
import net.bluemind.dav.server.proto.props.webdav.CurrentUserPrivilegeSet;
import net.bluemind.dav.server.proto.props.webdav.DisplayName;
import net.bluemind.dav.server.proto.props.webdav.Owner;
import net.bluemind.dav.server.proto.props.webdav.QuotaAvailableBytes;
import net.bluemind.dav.server.proto.props.webdav.QuotaUsedBytes;
import net.bluemind.dav.server.proto.props.webdav.ResourceType;
import net.bluemind.dav.server.proto.props.webdav.SupportedReportSet;
import net.bluemind.dav.server.proto.props.webdav.SyncToken;
import net.bluemind.dav.server.store.DavResource;
import net.bluemind.dav.server.store.ResType;

public class NotificationsEndoint extends DavResource {

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

		props.add(DisplayName.NAME);
		props.add(GetCTag.NAME);
		props.add(SyncToken.NAME);
		props.add(NotificationType.NAME);

		properties = ImmutableSet.copyOf(props);

		types = ImmutableList.of(Types.COL, Types.NOTIF);
	}

	public NotificationsEndoint(String path) {
		super(path, ResType.NOTIFICATIONS);
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
