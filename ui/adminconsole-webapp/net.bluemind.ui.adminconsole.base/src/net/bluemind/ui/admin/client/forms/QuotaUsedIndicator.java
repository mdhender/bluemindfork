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
package net.bluemind.ui.admin.client.forms;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;

public class QuotaUsedIndicator extends Composite {

	public static final String TYPE = "bm.ac.QuotaUsedIndicator";
	private HTMLPanel root;

	@UiField
	Label used;

	@UiField
	HTMLPanel usedBar;

	interface GeneralUiBinder extends UiBinder<HTMLPanel, QuotaUsedIndicator> {
	}

	private static GeneralUiBinder uiBinder = GWT.create(GeneralUiBinder.class);

	public QuotaUsedIndicator() {
		root = uiBinder.createAndBindUi(this);
		initWidget(root);
	}

	public void setQuotaUsed(int quotaUsed, int usedKb) {
		if (quotaUsed > 0) {
			this.used.setText(quotaUsed + " % (" + usedKb + " kb)");
		} else {
			this.used.setText(quotaUsed + " %");
		}
		String color = "#5fd85d";
		if (quotaUsed > 70 && quotaUsed < 96) {
			color = "#e0e032";
		} else if (quotaUsed > 95) {
			color = "#e04c4c";
		}
		this.usedBar.getElement().setAttribute("style", "width: " + quotaUsed + "%;background-color: " + color);
	}

}
