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
package net.bluemind.dav.server.proto.props.webdav;

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

public class GetContentType implements IPropertyValue {

	private static final Logger logger = LoggerFactory.getLogger(GetContentType.class);

	public static final QName NAME = new QName(NS.WEBDAV, "getcontenttype");

	private String type;

	@Override
	public QName getName() {
		return NAME;
	}

	@Override
	public void appendValue(Element parent) {
		parent.setTextContent(type);
	}

	public static IPropertyFactory factory() {
		return new IPropertyFactory() {
			@Override
			public IPropertyValue create() {
				return new GetContentType();
			}
		};
	}

	@Override
	public void fetch(LoggedCore lc, DavResource dr) {
		switch (dr.getResType()) {
		case VSTUFF_CONTAINER:
			type = "httpd/unix-directory";
			break;
		case VSTUFF:
			type = "text/calendar;charset=utf-8";
			break;
		default:
			throw new RuntimeException("Unknown contenttype for " + dr.getPath());
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
