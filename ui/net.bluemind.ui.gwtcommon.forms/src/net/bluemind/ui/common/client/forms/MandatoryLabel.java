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

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.Label;

public class MandatoryLabel extends Label {
	public static interface Resources extends ClientBundle {
		@Source("Common.css")
		Style editStyle();
	}

	public static interface Style extends CssResource {
		String labelMandatory();

		String inputMandatory();
	}

	protected static final Resources res = GWT.create(Resources.class);
	protected Style s;

	public MandatoryLabel() {
		super();

		s = res.editStyle();
		s.ensureInjected();

		addStyleName(s.labelMandatory());
	}

	public void setText(String label) {
		super.setText(label.endsWith(" *") ? label : label + " *");
	}
}
