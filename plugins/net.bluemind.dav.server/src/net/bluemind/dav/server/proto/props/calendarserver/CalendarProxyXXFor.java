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

import net.bluemind.core.container.api.ContainerSubscriptionDescriptor;
import net.bluemind.dav.server.Proxy;
import net.bluemind.dav.server.proto.IPropertyValue;
import net.bluemind.dav.server.store.DavResource;
import net.bluemind.dav.server.store.LoggedCore;
import net.bluemind.dav.server.store.Property;
import net.bluemind.dav.server.xml.DOMUtils;
import net.bluemind.user.api.IUserSubscription;

public abstract class CalendarProxyXXFor implements IPropertyValue {

	private static final Logger logger = LoggerFactory.getLogger(CalendarProxyXXFor.class);

	private List<ContainerSubscriptionDescriptor> subs;

	private List<Property> scope;

	@Override
	public final void appendValue(Element parent) {
		if (subs == null || scope == null) {
			return;
		}
		for (ContainerSubscriptionDescriptor ci : subs) {
			Element re = DOMUtils.createElement(parent, "d:response");

			String princ = Proxy.path + "/principals/__uids__/" + ci.owner + "/";
			DOMUtils.createElementAndText(re, "d:href", princ);
			Element pse = DOMUtils.createElement(re, "d:propstat");
			Element pre = DOMUtils.createElement(pse, "d:prop");
			DOMUtils.createElementAndText(pse, "d:status", "HTTP/1.1 200 OK");
			for (Property prop : scope) {
				QName qn = prop.getQName();
				Element ve = DOMUtils.createElement(pre, qn.getPrefix() + ":" + qn.getLocalPart());
				switch (qn.getLocalPart()) {
				case "displayname":
					ve.setTextContent(ci.ownerDisplayName);
					break;
				case "calendar-user-address-set":
					DOMUtils.createElementAndText(ve, "d:href", princ);
					break;
				case "email-address-set":
					// if (ci.getMail() != null) {
					// DOMUtils.createElementAndText(ve, "cso:email-address",
					// ci.getMail());
					// }
					break;
				default:
					logger.warn("Unsupported prop: {}", qn);
				}
			}
		}
	}

	@Override
	public final void fetch(LoggedCore lc, DavResource dr) throws Exception {
		logger.info("fetch ?!");
	}

	@Override
	public final void expand(LoggedCore lc, DavResource dr, List<Property> scope) throws Exception {
		if (!lc.getUser().uid.equals(dr.getUid())) {
			// do not return delegates of delegates
			return;
		}
		IUserSubscription subApi = lc.getCore().instance(IUserSubscription.class, lc.getDomain());
		List<ContainerSubscriptionDescriptor> cis = subApi.listSubscriptions(lc.getUser().uid, "calendar");
		this.subs = filterSubscriptionsByOwner(lc.getUser().uid, cis);
		this.scope = scope;
		logger.info("filtered {} calendars to {} owners", cis.size(), subs.size());
	}

	protected abstract List<ContainerSubscriptionDescriptor> filterSubscriptionsByOwner(String userUid,
			List<ContainerSubscriptionDescriptor> cis);

}
