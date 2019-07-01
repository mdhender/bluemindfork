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

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.Constants;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.Label;

public class Spinner extends Label {

	private Style s;
	private boolean visible;

	interface Res extends ClientBundle {
		@Source("Spinner.css")
		Style spinnerStyles();
	}

	interface Style extends CssResource {

		String spinner();

		String visible();

	}

	interface SpinnerConstants extends Constants {
		String loading();
	}

	private static final Res res = GWT.create(Res.class);
	private static final SpinnerConstants sc = GWT.create(SpinnerConstants.class);

	public Spinner() {
		super();
		setText(sc.loading());
		this.s = res.spinnerStyles();
		s.ensureInjected();
		setStyleName(s.spinner());
	}

	@Override
	public void setVisible(boolean visible) {
		if (visible) {
			addStyleName(s.visible());
			this.visible = true;
		} else {
			removeStyleName(s.visible());
			this.visible = false;
		}
	}

	@Override
	public boolean isVisible() {
		return visible;
	}
}
