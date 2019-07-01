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
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;

public abstract class ButtonBar extends Composite {

	public interface BBBundle extends ClientBundle {

		@Source("ButtonBar.css")
		BBStyle getStyle();

	}

	public interface BBStyle extends CssResource {

		String btn();

		String primaryAction();

		String stdAction();

		String hPanel();

	}

	public static final BBBundle bundle;
	public static final BBStyle style;

	static {
		bundle = GWT.create(BBBundle.class);
		style = bundle.getStyle();
		style.ensureInjected();
	}

	protected ButtonBar() {
	}

	protected Button newStdButton(String lbl) {
		Button b = new Button(lbl);
		b.setStyleName(style.btn());
		b.addStyleName(style.stdAction());
		return b;
	}

	protected Button newPrimaryButton(String lbl) {
		Button b = new Button(lbl);
		b.setStyleName(style.btn());
		b.addStyleName(style.primaryAction());
		return b;
	}

	public static Button primary(String lbl) {
		ButtonBar bb = new ButtonBar() {
		};
		return bb.newPrimaryButton(lbl);
	}

}
