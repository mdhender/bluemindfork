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
import java.util.Set;

import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import com.google.common.collect.ImmutableSet;

import net.bluemind.dav.server.proto.IPropertyValue;
import net.bluemind.dav.server.proto.NS;
import net.bluemind.dav.server.proto.QN;
import net.bluemind.dav.server.proto.props.IPropertyFactory;
import net.bluemind.dav.server.store.DavResource;
import net.bluemind.dav.server.store.LoggedCore;
import net.bluemind.dav.server.store.Property;
import net.bluemind.dav.server.xml.DOMUtils;

public class AllowedSharingModes implements IPropertyValue {
	public static final QName NAME = new QName(NS.CSRV_ORG, "allowed-sharing-modes");
	private static final Logger logger = LoggerFactory.getLogger(AllowedSharingModes.class);

	private Set<QName> sharingModes;

	@Override
	public QName getName() {
		return NAME;
	}

	public static IPropertyFactory factory() {
		return new IPropertyFactory() {
			@Override
			public IPropertyValue create() {
				return new AllowedSharingModes();
			}
		};
	}

	@Override
	public void appendValue(Element parent) {
		for (QName qn : sharingModes) {
			DOMUtils.createElement(parent, qn.getPrefix() + ":" + qn.getLocalPart());
		}
	}

	@Override
	public void fetch(LoggedCore lc, DavResource dr) throws Exception {
		logger.warn("Check if can manage acl on calendar...");
		if (dr.getUid().equals(lc.getUser().uid)) {
			this.sharingModes = ImmutableSet.of(QN.qn(NS.CSRV_ORG, "can-be-shared"));
		} else {
			this.sharingModes = ImmutableSet.of();
		}
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
