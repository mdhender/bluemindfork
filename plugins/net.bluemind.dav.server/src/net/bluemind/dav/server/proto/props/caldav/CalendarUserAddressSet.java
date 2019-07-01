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

import java.util.LinkedList;

import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.dav.server.Proxy;
import net.bluemind.dav.server.proto.IPropertyValue;
import net.bluemind.dav.server.proto.NS;
import net.bluemind.dav.server.proto.props.HrefSet;
import net.bluemind.dav.server.proto.props.IPropertyFactory;
import net.bluemind.dav.server.store.DavResource;
import net.bluemind.dav.server.store.LoggedCore;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;

public class CalendarUserAddressSet extends HrefSet {

	private static final Logger logger = LoggerFactory.getLogger(CalendarUserAddressSet.class);
	public static final QName NAME = new QName(NS.CALDAV, "calendar-user-address-set");

	public CalendarUserAddressSet() {
		super(NAME);
	}

	public static IPropertyFactory factory() {
		return new IPropertyFactory() {
			@Override
			public IPropertyValue create() {
				return new CalendarUserAddressSet();
			}
		};
	}

	@Override
	public void fetch(LoggedCore lc, DavResource dr) throws Exception {
		hrefs = new LinkedList<>();
		hrefs.add(Proxy.path + "/principals/__uids__/" + dr.getUid() + "/");
		hrefs.add("urn:uuid:" + dr.getUid());
		IDirectory service = lc.getCore().instance(IDirectory.class, lc.getDomain());
		DirEntry dirEntry = service.findByEntryUid(dr.getUid());
		if (dirEntry != null) {
			// hrefs.add(Proxy.path + "/principals/users/"
			// + u.getReservedBoxName() + "/");
			hrefs.add("mailto:" + dirEntry.email);
		} else {
			logger.warn("Can't fetch owner of {}", dr.getPath());
		}
	}
}
