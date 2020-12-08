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
import java.util.Map;

import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.dav.server.proto.IPropertyValue;
import net.bluemind.dav.server.proto.NS;
import net.bluemind.dav.server.proto.props.IPropertyFactory;
import net.bluemind.dav.server.store.DavResource;
import net.bluemind.dav.server.store.LoggedCore;
import net.bluemind.dav.server.store.Property;
import net.bluemind.dav.server.store.ResType;

public class CalendarColor implements IPropertyValue {
	public static final QName NAME = new QName(NS.APPLE_ICAL, "calendar-color");
	private static final Logger logger = LoggerFactory.getLogger(CalendarColor.class);
	private String symbolic;
	private String rgb;

	private static final BMCalColor[] allColors = BMCalColor.values();

	@Override
	public QName getName() {
		return NAME;
	}

	public static IPropertyFactory factory() {
		return new IPropertyFactory() {
			@Override
			public IPropertyValue create() {
				return new CalendarColor();
			}
		};
	}

	@Override
	public void appendValue(Element parent) {
		parent.setAttribute("symbolic-color", symbolic);
		parent.setTextContent(rgb);
	}

	@Override
	public void fetch(LoggedCore lc, DavResource dr) throws Exception {
		ContainerDescriptor cal = lc.vStuffContainer(dr);
		String stringColor = cal.settings.get("dav_color");
		if (stringColor != null) {
			int idx = stringColor.indexOf('#');
			symbolic = stringColor.substring(0, idx);
			rgb = stringColor.substring(idx);
			logger.info("[{}] will use custom color {}{} ", dr.getPath(), symbolic, rgb);
		} else {
			// generate & update
			int index = Math.abs(dr.getPath().hashCode()) % allColors.length;
			BMCalColor cc = allColors[index];
			symbolic = cc.getSymbolic();
			rgb = cc.getRgb();
			logger.info("[{}] will use generated color {} ({}) {}{}", dr.getPath(), cc, index, symbolic, rgb);
		}
	}

	@Override
	public void expand(LoggedCore lc, DavResource dr, List<Property> scope) throws Exception {
		logger.info("expand");
	}

	@Override
	public void set(LoggedCore lc, DavResource dr, Element value) throws Exception {
		logger.info("[{}] set on {}", dr.getResType(), dr.getPath());
		// <D:calendar-color xmlns:D="http://apple.com/ns/ical/"
		// symbolic-color="orange">#F64F00FF</D:calendar-color>
		symbolic = value.getAttribute("symbolic-color");
		rgb = value.getTextContent();
		String contSettingValue = symbolic + rgb;
		if (dr.getResType() == ResType.VSTUFF_CONTAINER) {
			ContainerDescriptor desc = lc.vStuffContainer(dr);
			IContainerManagement mgmt = lc.getCore().instance(IContainerManagement.class, desc.uid);
			Map<String, String> settings = desc.settings;
			settings.put("dav_color", contSettingValue);
			mgmt.setPersonalSettings(settings);
			logger.info("set dav_color to '{}'", contSettingValue);
		}
	}

}
