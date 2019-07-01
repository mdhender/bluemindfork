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
package net.bluemind.ui.mailbox.filter;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.VerticalAlign;
import com.google.gwt.editor.client.IsEditor;
import com.google.gwt.editor.client.LeafValueEditor;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.ListBox;

import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.mailbox.api.MailFilter.Forwarding;
import net.bluemind.ui.mailbox.filter.multipleforward.MultipleForward;

public class MailForwardEdit extends Composite implements IsEditor<LeafValueEditor<MailFilter.Forwarding>> {

	private static final MailForwardConstants constants = GWT.create(MailForwardConstants.class);

	private CheckBox forwardCheckBox;
	private MultipleForward forwardTo;
	private ListBox lb;

	private final LeafValueEditor<MailFilter.Forwarding> editor = new LeafValueEditor<MailFilter.Forwarding>() {

		@Override
		public void setValue(Forwarding value) {
			if (value == null) {
				disabled();
				return;
			}

			if (value.enabled) {
				enabled();
			} else {
				disabled();
			}

			forwardCheckBox.setValue(value.enabled);
			forwardTo.setRecipients(value.emails);

			if (value.localCopy) {
				lb.setSelectedIndex(0);
			} else {
				lb.setSelectedIndex(1);
			}
		}

		@Override
		public Forwarding getValue() {
			Forwarding ret = new Forwarding();
			ret.enabled = forwardCheckBox.getValue();

			ret.emails.clear();
			ret.emails.addAll(forwardTo.getRecipients());

			ret.localCopy = lb.getSelectedIndex() == 0;
			return ret;
		}

	};

	public MailForwardEdit() {
		super();
		FlexTable container = new FlexTable();

		forwardCheckBox = new CheckBox(constants.mailForwardTo());
		forwardCheckBox.getElement().setId("forward-allow");
		forwardCheckBox.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				if (forwardCheckBox.getValue()) {
					enabled();
				} else {
					disabled();
				}
			}
		});

		lb = new ListBox();
		lb.getElement().setId("forward-action");
		lb.addItem(constants.mailForwardCopy(), "copy");
		lb.addItem(constants.mailForwardNoCopy(), "nocopy");

		forwardTo = new MultipleForward();
		forwardTo.setWidth("400px");

		int row = 0;
		container.setWidget(row++, 0, forwardCheckBox);
		container.getFlexCellFormatter().setColSpan(0, 0, 2);
		container.setWidget(row, 0, forwardTo);
		container.setWidget(row, 1, lb);
		container.getRowFormatter().getElement(row).getStyle().setVerticalAlign(VerticalAlign.TOP);
		initWidget(container);

		disabled();
	}

	private void disabled() {
		// forwardTo.setValue(null);
		forwardTo.setEnabled(false);
		lb.setEnabled(false);
		lb.setSelectedIndex(0);
	}

	private void enabled() {
		forwardTo.setEnabled(true);
		lb.setEnabled(true);
		lb.setSelectedIndex(0);
	}

	@Override
	public LeafValueEditor<Forwarding> asEditor() {
		return editor;
	}

}
