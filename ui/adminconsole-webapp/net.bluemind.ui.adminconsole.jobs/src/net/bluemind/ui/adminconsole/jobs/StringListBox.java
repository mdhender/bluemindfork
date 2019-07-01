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
package net.bluemind.ui.adminconsole.jobs;

import java.util.HashMap;

import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;

import net.bluemind.ui.common.client.forms.TrPanel;

public class StringListBox extends Composite {
	private TrPanel tr;
	private ListBox lb;
	private Label title;

	private HashMap<String, Integer> lbTab;

	public StringListBox() {

		this.lbTab = new HashMap<String, Integer>();

		tr = new TrPanel();
		tr.addStyleName("setting");
		title = new Label();
		tr.add(title, "label");
		lb = new ListBox();
		tr.add(lb, "form");

		initWidget(tr);

	}

	public StringListBox(String titleText) {
		this();
		setTitle(titleText);
	}

	public String getTitleText() {
		return title.getText();
	}

	public void setTitleText(String titleText) {
		title.setText(titleText);
	}

	public void reset() {
		lb.clear();
		lbTab.clear();
	}

	public void addItem(String textItem) {
		lb.addItem(textItem);
		lbTab.put(textItem, lbTab.size());
	}

	public void addItem(String key, String value) {
		lb.addItem(value, key);
		lbTab.put(key, lbTab.size());
	}

	public void setSelectedItem(String textItem) {
		lb.setSelectedIndex(lbTab.get(textItem));
	}

	public String getSelected() {
		return lb.getValue(lb.getSelectedIndex());
	}

	public String getSelectedValue() {
		return lb.getValue(lb.getSelectedIndex());
	}

	public HandlerRegistration addChangeHandler(ChangeHandler ch) {
		return lb.addChangeHandler(ch);
	}

	/**
	 * Does not fire change handlers.
	 * 
	 * @param index
	 */
	public void setSelectedIndex(int index) {
		lb.setSelectedIndex(index);
	}
}
