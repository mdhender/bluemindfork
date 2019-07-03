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

import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ContainerModifiableDescriptor;
import net.bluemind.dav.server.proto.IPropertyValue;
import net.bluemind.dav.server.proto.NS;
import net.bluemind.dav.server.proto.QN;
import net.bluemind.dav.server.proto.props.IPropertyFactory;
import net.bluemind.dav.server.store.BookUtils;
import net.bluemind.dav.server.store.DavResource;
import net.bluemind.dav.server.store.LoggedCore;
import net.bluemind.dav.server.store.Property;
import net.bluemind.dav.server.store.ResType;

public class DisplayName implements IPropertyValue {

	private static final Logger logger = LoggerFactory.getLogger(DisplayName.class);

	public static final QName NAME = QN.qn(NS.WEBDAV, "displayname");

	private String dn;

	@Override
	public QName getName() {
		return NAME;
	}

	@Override
	public void appendValue(Element parent) {
		parent.setTextContent(dn);
	}

	public static IPropertyFactory factory() {
		return new IPropertyFactory() {
			@Override
			public IPropertyValue create() {
				return new DisplayName();
			}
		};
	}

	@Override
	public void fetch(LoggedCore lc, DavResource dr) {
		switch (dr.getResType()) {
		case CALENDAR:
		case PRINCIPAL:
			dn = lc.principalDirEntry(dr).displayName;
			break;
		case NOTIFICATIONS:
			dn = "notification";
			break;
		case SCHEDULE_INBOX:
			dn = "inbox";
			break;
		case VSTUFF_CONTAINER:
			dn = lc.vStuffContainer(dr).name;
			break;
		case VCARDS_CONTAINER:
			ContainerDescriptor cardsContainer = BookUtils.addressbook(lc, dr);
			dn = cardsContainer == null ? String.format("[missing vcards container for '%s']", dr.getPath())
					: cardsContainer.name;
			break;
		default:
			logger.error("No display name for {}", dr.getPath());
			dn = "bm.unnamed";
			break;
		}
	}

	@Override
	public void expand(LoggedCore lc, DavResource dr, List<Property> scope) throws Exception {
		logger.info("expand");
	}

	@Override
	public void set(LoggedCore lc, DavResource dr, Element value) throws Exception {
		logger.info("[{}] set on {}", dr.getResType(), dr.getPath());
		if (dr.getResType() == ResType.VSTUFF_CONTAINER) {
			ContainerDescriptor desc = lc.vStuffContainer(dr);
			IContainers contApi = lc.getCore().instance(IContainers.class);
			ContainerModifiableDescriptor cm = new ContainerModifiableDescriptor();
			cm.defaultContainer = desc.defaultContainer;
			cm.name = value.getTextContent();
			contApi.update(desc.uid, cm);
			logger.info("Container " + desc.uid + " renamed to " + cm.name);
		}
	}

}
