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
package net.bluemind.ui.common.client.forms;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.HTML;

/**
 * @author mehdi
 * 
 */
public class ColorDot extends HTML {

	public static interface Resources extends ClientBundle {
		@Source("ColorDot.css")
		Style style();

	}

	public static interface Style extends CssResource {
		String dot();

	}

	protected static final Resources res = GWT.create(Resources.class);

	private Color color;
	protected Style s;

	public ColorDot(Color color) {
		this();
		setColor(color);
	}

	public ColorDot() {
		s = res.style();
		init();

	}

	private void init() {
		s.ensureInjected();
		setStyleName(s.dot());

	}

	/**
	 * 
	 */
	public void setColor(Color value) {
		color = value;
		if (color != null) {
			getElement().getStyle().setBackgroundColor("#" + color.getRGB());
			getElement().getStyle().setBorderColor("#" + color.getRGB());
		} else {
			getElement().getStyle().setBackgroundColor("transparent");
			getElement().getStyle().setBorderColor("#000");
		}
	}

	public Color getValue() {
		return color;
	}

}
