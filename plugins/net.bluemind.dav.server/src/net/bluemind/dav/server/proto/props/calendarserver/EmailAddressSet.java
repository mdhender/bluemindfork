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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import com.google.common.collect.ImmutableList;

import net.bluemind.core.api.Email;
import net.bluemind.dav.server.proto.IPropertyValue;
import net.bluemind.dav.server.proto.NS;
import net.bluemind.dav.server.proto.props.IPropertyFactory;
import net.bluemind.dav.server.store.DavResource;
import net.bluemind.dav.server.store.LoggedCore;
import net.bluemind.dav.server.store.Path;
import net.bluemind.dav.server.store.Property;
import net.bluemind.dav.server.xml.DOMUtils;
import net.bluemind.directory.api.IDirectory;

public class EmailAddressSet implements IPropertyValue {
	public static final QName NAME = new QName(NS.CSRV_ORG, "email-address-set");

	private static final Logger logger = LoggerFactory.getLogger(EmailAddressSet.class);
	private Collection<net.bluemind.core.api.Email> emails;

	@Override
	public QName getName() {
		return NAME;
	}

	public static IPropertyFactory factory() {
		return new IPropertyFactory() {
			@Override
			public IPropertyValue create() {
				return new EmailAddressSet();
			}
		};
	}

	@Override
	public void appendValue(Element parent) {
		for (net.bluemind.core.api.Email e : emails) {
			Element eme = DOMUtils.createElement(parent, "cso:email-address");
			eme.setTextContent(e.address);
		}
	}

	@Override
	public void fetch(LoggedCore lc, DavResource dr) throws Exception {
		if (Path.isVStuffContainer(dr.getPath())) {
			IDirectory api = lc.getCore().instance(IDirectory.class, lc.getDomain());
			emails = new ArrayList<net.bluemind.core.api.Email>();
			emails.add(Email.create(api.findByEntryUid(dr.getUid()).email, true));
		} else {
			emails = ImmutableList.of();
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
