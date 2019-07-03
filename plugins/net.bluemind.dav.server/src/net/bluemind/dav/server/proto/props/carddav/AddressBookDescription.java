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
package net.bluemind.dav.server.proto.props.carddav;

import java.util.List;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.dav.server.proto.IPropertyValue;
import net.bluemind.dav.server.proto.NS;
import net.bluemind.dav.server.proto.props.IPropertyFactory;
import net.bluemind.dav.server.store.BookUtils;
import net.bluemind.dav.server.store.DavResource;
import net.bluemind.dav.server.store.LoggedCore;
import net.bluemind.dav.server.store.Property;

public class AddressBookDescription implements IPropertyValue {

	public static final QName NAME = new QName(NS.CARDDAV, "addressbook-description");
	private String dn;

	@Override
	public QName getName() {
		return NAME;
	}

	@Override
	public void appendValue(Element parent) {
		if (dn != null) {
			parent.setTextContent(dn);
		} else {
			parent.setTextContent("unknown");
		}
	}

	public static IPropertyFactory factory() {
		return new IPropertyFactory() {
			@Override
			public IPropertyValue create() {
				return new AddressBookDescription();
			}
		};
	}

	@Override
	public void fetch(LoggedCore lc, DavResource dr) throws Exception {
		ContainerDescriptor cardsContainer = BookUtils.addressbook(lc, dr);
		dn = cardsContainer == null ? String.format("[missing vcards container for '%s']", dr.getPath())
				: cardsContainer.name;

	}

	@Override
	public void expand(LoggedCore lc, DavResource dr, List<Property> scope) throws Exception {

	}

	@Override
	public void set(LoggedCore lc, DavResource dr, Element value) throws Exception {

	}

}
