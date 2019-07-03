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
package net.bluemind.ui.adminconsole.base.client;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.Widget;

public class BulletList extends ComplexPanel {

	private Element list;

	public BulletList() {
		this.list = DOM.createElement("ul");
		setElement(list);
	}

	@Override
	public void add(Widget child) {
		add(child, (String) null);
	}

	public void add(Widget child, String sName) {
		Element li = DOM.createElement("li");
		list.appendChild(li);
		super.add(child, li);
		if (sName != null) {
			li.addClassName(sName);
		}
	}

}
