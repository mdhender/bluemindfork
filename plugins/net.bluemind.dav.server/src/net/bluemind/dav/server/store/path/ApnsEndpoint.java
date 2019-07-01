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

import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import net.bluemind.dav.server.store.DavResource;
import net.bluemind.dav.server.store.ResType;

public class ApnsEndpoint extends DavResource {

	private static final Set<QName> properties = ImmutableSet.of();
	private static final List<QName> types = ImmutableList.of();

	public ApnsEndpoint(String path) {
		super(path, ResType.APNS);
	}

	@Override
	public boolean hasProperty(QName prop) {
		return false;
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
