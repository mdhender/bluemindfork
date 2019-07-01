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
package net.bluemind.dav.server.proto.props.calendarserver;

import java.util.List;

import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import net.bluemind.dav.server.proto.IPropertyValue;
import net.bluemind.dav.server.proto.NS;
import net.bluemind.dav.server.proto.props.IPropertyFactory;
import net.bluemind.dav.server.store.DavResource;
import net.bluemind.dav.server.store.LoggedCore;
import net.bluemind.dav.server.store.Property;

/**
 * 
 * Purpose: Used to show to whom a calendar has been shared.
 * 
 * Protected: This property MUST be protected.
 * 
 * PROPFIND behavior: This property SHOULD NOT be returned by a PROPFIND allprop
 * request (as defined in Section 14.2 of [RFC4918]).
 * 
 * COPY/MOVE behavior: This property value MUST be preserved in COPY and MOVE
 * operations.
 * 
 * Description: This WebDAV property is present on a calendar collection
 * resource that has been shared by the owner, or on the calendar collection
 * resources of the sharees of the calendar. It provides a list of users to whom
 * the calendar has been shared, along with the "status" of the sharing invites
 * sent to each user. In addition, servers SHOULD include a CS:organizer XML
 * element on calendar collection resources of the sharees to provide clients
 * with a fast way to determine who the sharer is. A server's local privacy
 * policy may prevent sharees from knowing about other sharees on a shared
 * calendar. If that is so server will not include CS:user XML elements for
 * other sharees.
 */
public class Invite implements IPropertyValue {
	public static final QName NAME = new QName(NS.CSRV_ORG, "invite");
	private static final Logger logger = LoggerFactory.getLogger(Invite.class);

	@Override
	public QName getName() {
		return NAME;
	}

	public static IPropertyFactory factory() {
		return new IPropertyFactory() {
			@Override
			public IPropertyValue create() {
				return new Invite();
			}
		};
	}

	@Override
	public void appendValue(Element parent) {
		logger.warn("Nothing to append");
	}

	@Override
	public void fetch(LoggedCore lc, DavResource dr) throws Exception {
		logger.warn("Not showing to whom we shared {}", dr.getPath());
	}

	@Override
	public void expand(LoggedCore lc, DavResource dr, List<Property> scope) throws Exception {
		logger.info("expand");
	}

	@Override
	public void set(LoggedCore lc, DavResource dr, Element value) throws Exception {
		logger.info("[{}] set on {}", dr.getResType(), dr.getPath());
	}

}
