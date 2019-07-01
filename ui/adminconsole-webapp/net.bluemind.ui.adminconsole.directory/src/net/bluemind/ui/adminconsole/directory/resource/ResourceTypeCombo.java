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
package net.bluemind.ui.adminconsole.directory.resource;

import java.util.HashMap;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.resource.api.type.ResourceType;
import net.bluemind.resource.api.type.gwt.endpoint.ResourceTypesGwtEndpoint;
import net.bluemind.ui.adminconsole.directory.resourcetype.l10n.ResourceTypeConstants;
import net.bluemind.ui.common.client.forms.Ajax;
import net.bluemind.ui.common.client.forms.TrPanel;

public class ResourceTypeCombo extends Composite {

	private TrPanel tr;
	private ListBox lb;
	private Label title;
	private HashMap<String, Integer> lbTab;
	private String locale;

	public ResourceTypeCombo() {
		locale = LocaleInfo.getCurrentLocale().getLocaleName();
		if (locale.length() > 2) {
			locale = locale.substring(0, 2);
		}
		lbTab = new HashMap<String, Integer>();
		tr = new TrPanel();
		tr.addStyleName("setting");
		title = new Label();
		tr.add(title, "label");
		lb = new ListBox();
		lb.getElement().setId("resource-type-combo");
		tr.add(lb, "form");
		initWidget(tr);
	}

	public void setReadOnly(boolean readOnly) {
		lb.setEnabled(!readOnly);
	}

	public void init(String domainUid) {
		init(domainUid, null);
	}

	public void init(String domainUid, final String currentValue) {

		addItem("", ResourceTypeConstants.INST.none());
		new ResourceTypesGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid)
				.getTypes(new AsyncHandler<List<ResourceType>>() {

					@Override
					public void success(List<ResourceType> types) {
						lb.clear();
						lbTab.clear();
						addItems(types);
						if (currentValue != null) {
							setSelectedItem(currentValue);

						}
					}

					@Override
					public void failure(Throwable e) {
						GWT.log("failed to fetch resource type");
					}
				});
	}

	private void addItems(List<ResourceType> types) {
		for (ResourceType type : types) {
			String[] labels = type.label.split("\n");
			String l = null;
			for (String label : labels) {
				if (label.startsWith(locale + "::")) {
					String[] i18nLabel = label.split("::");
					l = i18nLabel[1];
				} else {
					l = label;
				}
			}
			addItem(type.identifier, l);
		}
	}

	public String getTitleText() {
		return title.getText();
	}

	public void setTitleText(String titleText) {
		title.setText(titleText);
	}

	public void addItem(String key, String value) {
		lb.addItem(value, key);
		lbTab.put(key, lbTab.size());
		GWT.log(" ADD RTYPE >  " + key + " " + value);

	}

	public void setSelectedItem(String key) {
		GWT.log(" SELECT RTYPE >  " + key);

		if (lbTab.containsKey(key)) {
			lb.setSelectedIndex(lbTab.get(key));
		} else {
			lb.setSelectedIndex(0);
		}
	}

	public String getSelectedValue() {
		if (lb.getSelectedIndex() == -1) {
			return null;
		} else {
			return lb.getValue(lb.getSelectedIndex());
		}
	}

	public ListBox getListBox() {
		return lb;
	}

	public void reset() {
		lb.clear();
		lbTab.clear();
	}
}
