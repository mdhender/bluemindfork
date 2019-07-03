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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;

import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;
import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.mailbox.api.gwt.js.JsMailFilterForwarding;
import net.bluemind.mailbox.api.gwt.serder.MailFilterForwardingGwtSerDer;

public class MailForwardEditor extends CompositeGwtWidgetElement {

	public static final String TYPE = "bm.mail.MailForwardEditor";
	private MailForwardEdit editor;

	public MailForwardEditor() {
		editor = new MailForwardEdit();

		FlowPanel panel = new FlowPanel();
		Label title = new Label(MailForwardConstants.INST.forwardTitle());
		title.setStyleName("sectionTitle");
		panel.add(title);
		panel.add(editor);

		initWidget(panel);
	}

	@Override
	public void loadModel(JavaScriptObject m) {
		MailSettingsModel model = MailSettingsModel.get(m);
		MailFilter mf = model.getMailFilter();

		if (mf == null) {
			asWidget().setVisible(false);
		} else {
			asWidget().setVisible(true);
			if (mf.forwarding != null) {
				editor.asEditor().setValue(mf.forwarding);
			}
		}
	}

	@Override
	public void saveModel(JavaScriptObject m) {
		MailSettingsModel model = MailSettingsModel.get(m);

		if (model.getJsMailFilter() != null) {
			MailFilter.Forwarding forwarding = editor.asEditor().getValue();
			model.getJsMailFilter().setForwarding(new MailFilterForwardingGwtSerDer().serialize(forwarding).isObject()
					.getJavaScriptObject().<JsMailFilterForwarding> cast());
		}
	}

	public static void registerType() {

		GwtWidgetElement.register(TYPE, new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

			@Override
			public IGwtWidgetElement create(WidgetElement e) {
				return new MailForwardEditor();
			}
		});
	}
}
