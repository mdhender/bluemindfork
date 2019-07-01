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
package net.bluemind.dav.server.proto.props.appleical;

import java.util.List;

import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.dav.server.proto.IPropertyValue;
import net.bluemind.dav.server.proto.NS;
import net.bluemind.dav.server.proto.props.IPropertyFactory;
import net.bluemind.dav.server.store.DavResource;
import net.bluemind.dav.server.store.LoggedCore;
import net.bluemind.dav.server.store.Property;

public class CalendarOrder implements IPropertyValue {
	public static final QName NAME = new QName(NS.APPLE_ICAL, "calendar-order");
	private static final Logger logger = LoggerFactory.getLogger(CalendarOrder.class);
	private int order;

	@Override
	public QName getName() {
		return NAME;
	}

	public static IPropertyFactory factory() {
		return new IPropertyFactory() {
			@Override
			public IPropertyValue create() {
				return new CalendarOrder();
			}
		};
	}

	@Override
	public void appendValue(Element parent) {
		logger.error("append not implemented");
		parent.setTextContent("" + order);
	}

	@Override
	public void fetch(LoggedCore lc, DavResource dr) throws Exception {
		logger.warn("Cal Order is not a server side thing in BM");
		String p = dr.getPath();
		ContainerDescriptor cal = lc.vStuffContainer(dr);
		if ("calendar".equals(cal.type)) {
			order = 2;
		} else if ("todolist".equals(cal.type)) {
			order = 1;
		} else {
			logger.error("No hardcoded calendar-order for {}", p);
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
