/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.ui.common.client.forms;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.CheckBox;

public class SwitchButton extends CheckBox {

	public static interface Resources extends ClientBundle {
		@Source("SwitchButton.css")
		Style style();
	}

	public static interface Style extends CssResource {
		String switchButton();
	}

	protected static final Resources res = GWT.create(Resources.class);
	protected Style s;

	public SwitchButton(String name, boolean value, String on, String off) {
		super();

		s = res.style();
		s.ensureInjected();

		setName(name);
		setValue(value);

		// nodes are:
		// <input name="cb" type=checkbox>
		// <label for="cb"/>

		NodeList<Node> nodes = getElement().getChildNodes();
		Element input = (Element) nodes.getItem(0);
		input.setAttribute("class", s.switchButton());

		Element label = (Element) nodes.getItem(1);
		label.setAttribute("label-on", on);
		label.setAttribute("label-off", off);

	}

}
