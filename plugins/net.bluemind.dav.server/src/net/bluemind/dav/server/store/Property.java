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
package net.bluemind.dav.server.store;

import java.util.LinkedList;
import java.util.List;

import javax.xml.namespace.QName;

import net.bluemind.dav.server.proto.IPropertyValue;

public class Property {

	private List<Property> children;
	private IPropertyValue value;

	private QName qName;

	public Property() {
		this.children = new LinkedList<>();
	}

	public void addChild(Property p) {
		children.add(p);
	}

	public List<Property> getChildren() {
		return children;
	}

	public QName getQName() {
		return qName;
	}

	public void setQName(QName qName) {
		this.qName = qName;
	}

	public IPropertyValue getValue() {
		return value;
	}

	public void setValue(IPropertyValue value) {
		this.value = value;
	}

}
