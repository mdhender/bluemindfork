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

import com.google.common.collect.ImmutableList;

import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.dav.server.proto.IPropertyValue;
import net.bluemind.dav.server.proto.NS;
import net.bluemind.dav.server.proto.props.IPropertyFactory;
import net.bluemind.dav.server.store.DavResource;
import net.bluemind.dav.server.store.LoggedCore;
import net.bluemind.dav.server.store.Path;
import net.bluemind.dav.server.store.Property;
import net.bluemind.dav.server.xml.DOMUtils;

/**
 * Used by iCal on Lion
 */
public class AllowedCalendarComponentSet implements IPropertyValue {
	public static final QName NAME = new QName(NS.CSRV_ORG, "allowed-calendar-component-set");

	private static final Logger logger = LoggerFactory.getLogger(AllowedCalendarComponentSet.class);

	private List<String> components;

	@Override
	public QName getName() {
		return NAME;
	}

	public static IPropertyFactory factory() {
		return new IPropertyFactory() {
			@Override
			public IPropertyValue create() {
				return new AllowedCalendarComponentSet();
			}
		};
	}

	@Override
	public void appendValue(Element parent) {
		for (String c : components) {
			Element comp = DOMUtils.createElement(parent, "cal:comp");
			comp.setAttribute("name", c);
		}
	}

	@Override
	public void fetch(LoggedCore lc, DavResource dr) throws Exception {
		String p = dr.getPath();
		ContainerDescriptor cal = lc.vStuffContainer(dr);
		if ("calendar".equals(cal.type)) {
			components = ImmutableList.of("VEVENT");
		} else if ("todolist".equals(cal.type)) {
			components = ImmutableList.of("VTODO");
		} else if (Path.isScheduleInbox(p)) {
			components = ImmutableList.of("VEVENT", "VTODO", "VTIMEZONE", "VFREEBUSY");
		} else if (Path.isScheduleOutbox(p)) {
			components = ImmutableList.of("VEVENT", "VTODO", "VTIMEZONE", "VFREEBUSY");
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
