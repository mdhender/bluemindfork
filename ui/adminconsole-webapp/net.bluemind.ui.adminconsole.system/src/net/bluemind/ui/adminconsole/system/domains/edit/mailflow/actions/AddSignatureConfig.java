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
package net.bluemind.ui.adminconsole.system.domains.edit.mailflow.actions;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.Widget;

import net.bluemind.ui.adminconsole.system.domains.l10n.DomainConstants;

public class AddSignatureConfig extends Composite implements MailflowActionConfig {
	private static final DomainConstants TEXTS = GWT.create(DomainConstants.class);

	Grid tbl = new Grid();

	public AddSignatureConfig() {
		tbl = new Grid(5, 2);
		tbl.setCellPadding(10);
		tbl.setWidget(0, 0, new Label(TEXTS.isDisclaimer()));
		tbl.setWidget(1, 0, new Label(TEXTS.removePrevious()));
		tbl.setWidget(2, 0, new Label(TEXTS.usePlaceholder()));
		tbl.setWidget(3, 0, new Label("Text"));
		tbl.setWidget(4, 0, new Label("HTML"));
		CheckBox isDisclaimer = new CheckBox();
		CheckBox removePrevious = new CheckBox();
		CheckBox usePlaceholder = new CheckBox();
		TextArea plain = new TextArea();
		TextArea html = new TextArea();
		plain.setWidth("450px");
		html.setWidth("450px");
		tbl.setWidget(0, 1, isDisclaimer);
		tbl.setWidget(1, 1, usePlaceholder);
		tbl.setWidget(2, 1, removePrevious);
		tbl.setWidget(3, 1, plain);
		tbl.setWidget(4, 1, html);
		this.initWidget(tbl);
	}

	@Override
	public Map<String, String> get() {
		Map<String, String> values = new HashMap<>();
		boolean isDisclaimer = !((CheckBox) tbl.getWidget(0, 1)).getValue();
		values.put("isDisclaimer", (Boolean.toString(isDisclaimer)));
		values.put("usePlaceholder", ((CheckBox) tbl.getWidget(1, 1)).getValue().toString());
		values.put("removePrevious", ((CheckBox) tbl.getWidget(2, 1)).getValue().toString());
		values.put("plain", ((TextArea) tbl.getWidget(3, 1)).getText());
		values.put("html", ((TextArea) tbl.getWidget(4, 1)).getText());
		return values;
	}

	@Override
	public Widget getWidget() {
		return this;
	}

	@Override
	public void set(Map<String, String> config) {
		((CheckBox) tbl.getWidget(0, 1)).setValue(!Boolean.valueOf(config.get("isDisclaimer")));
		((CheckBox) tbl.getWidget(1, 1)).setValue(Boolean.valueOf(config.get("usePlaceholder")));
		((CheckBox) tbl.getWidget(2, 1)).setValue(Boolean.valueOf(config.get("removePrevious")));
		((TextArea) tbl.getWidget(3, 1)).setText(config.get("plain"));
		((TextArea) tbl.getWidget(4, 1)).setText(config.get("html"));
	}

	@Override
	public String getIdentifier() {
		return "AddSignatureAction";
	}

}
