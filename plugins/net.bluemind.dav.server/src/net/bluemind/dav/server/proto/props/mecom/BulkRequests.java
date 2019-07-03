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
package net.bluemind.dav.server.proto.props.mecom;

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
import net.bluemind.dav.server.xml.DOMUtils;

public class BulkRequests implements IPropertyValue {

	public static final QName NAME = new QName(NS.ME_COM, "bulk-requests");

	private static final Logger logger = LoggerFactory.getLogger(BulkRequests.class);

	@Override
	public QName getName() {
		return NAME;
	}

	public static IPropertyFactory factory() {
		return new IPropertyFactory() {
			@Override
			public IPropertyValue create() {
				return new BulkRequests();
			}
		};
	}

	@Override
	public void appendValue(Element parent) {
		Element simple = DOMUtils.createElement(parent, "me:simple");
		DOMUtils.createElementAndText(simple, "me:max-resources", "100");
		DOMUtils.createElementAndText(simple, "me:max-bytes", "10485760");

		Element crud = DOMUtils.createElement(parent, "me:crud");
		DOMUtils.createElementAndText(crud, "me:max-resources", "100");
		DOMUtils.createElementAndText(crud, "me:max-bytes", "10485760");
	}

	@Override
	public void fetch(LoggedCore lc, DavResource dr) throws Exception {
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
