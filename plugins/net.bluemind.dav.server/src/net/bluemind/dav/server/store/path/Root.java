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
import net.bluemind.dav.server.proto.props.webdav.CurrentUserPrincipal;
import net.bluemind.dav.server.proto.props.webdav.GroupMembership;
import net.bluemind.dav.server.proto.props.webdav.ResourceType;
import net.bluemind.dav.server.store.DavResource;
import net.bluemind.dav.server.store.ResType;

public class Root extends DavResource {

	private static final Set<QName> properties;
	private static final List<QName> types;

	static {
		Set<QName> props = new HashSet<>();
		props.add(CurrentUserPrincipal.NAME);
		props.add(ResourceType.NAME);
		props.add(GroupMembership.NAME);
		properties = ImmutableSet.copyOf(props);

		types = ImmutableList.of(Types.COL);
	}

	public Root(String path) {
		super(path, ResType.ROOT);
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
