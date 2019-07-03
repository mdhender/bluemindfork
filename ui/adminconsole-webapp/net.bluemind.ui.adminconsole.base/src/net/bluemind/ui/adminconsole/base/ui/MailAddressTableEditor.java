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
package net.bluemind.ui.adminconsole.base.ui;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.editor.client.IsEditor;
import com.google.gwt.editor.client.LeafValueEditor;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.user.client.ui.Composite;

import net.bluemind.core.api.gwt.js.JsEmail;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;

public class MailAddressTableEditor extends Composite implements IsEditor<LeafValueEditor<JsArray<JsEmail>>> {

	private MailAddressTable widget;

	private LeafValueEditor<JsArray<JsEmail>> editor = new LeafValueEditor<JsArray<JsEmail>>() {

		@Override
		public void setValue(JsArray<JsEmail> value) {
			widget.setValue(value);
		}

		@Override
		public JsArray<JsEmail> getValue() {
			return widget.getValue();
		}
	};

	@UiConstructor
	public MailAddressTableEditor(int size, boolean hasImplicitEmail) {
		widget = new MailAddressTable(size, hasImplicitEmail);

		initWidget(widget);
	}

	public void setDomain(ItemValue<Domain> d) {
		widget.setDomain(d);
	}

	@Override
	public LeafValueEditor<JsArray<JsEmail>> asEditor() {
		return editor;
	}

	@Override
	public MailAddressTable asWidget() {
		return widget;
	}

}
