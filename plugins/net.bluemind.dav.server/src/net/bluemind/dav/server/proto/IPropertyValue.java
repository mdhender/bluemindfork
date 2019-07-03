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
package net.bluemind.dav.server.proto;

import java.util.List;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import net.bluemind.dav.server.store.DavResource;
import net.bluemind.dav.server.store.LoggedCore;
import net.bluemind.dav.server.store.Property;

public interface IPropertyValue {

	QName getName();

	void appendValue(Element parent);

	void fetch(LoggedCore lc, DavResource dr) throws Exception;

	void expand(LoggedCore lc, DavResource dr, List<Property> scope) throws Exception;

	void set(LoggedCore lc, DavResource dr, Element value) throws Exception;
}
