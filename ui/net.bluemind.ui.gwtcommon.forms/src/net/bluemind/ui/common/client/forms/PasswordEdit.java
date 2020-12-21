/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.user.client.ui.TextBox;

public class PasswordEdit extends TextBox {
	private String password = "";

	public PasswordEdit() {
		super();
		addFocusHandler(this::focusHandler);
		addBlurHandler(this::blurHandler);
	}

	PasswordEdit(Element element, String styleName) {
		super(element);
		if (styleName != null) {
			setStyleName(styleName);
		}

		addFocusHandler(this::focusHandler);
		addBlurHandler(this::blurHandler);
	}

	@Override
	public String getValue() {
		return password;
	}

	@Override
	public void setValue(String value) {
		password = value;
		super.setValue(generateStars(password));
	}

	private void blurHandler(BlurEvent event) {
		password = super.getValue();
		super.setValue(generateStars(password));
	}

	private void focusHandler(FocusEvent event) {
		super.setValue(password);
	}

	private String generateStars(String password) {
		if (password == null || password.equals("")) {
			return "";
		}

		String stars = "";
		for (int i = 0; i < password.length(); i++) {
			stars += "\u25CF";
		}

		return stars;
	}
}
