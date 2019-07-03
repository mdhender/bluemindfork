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
package net.bluemind.ui.adminconsole.directory.user;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.user.client.ui.HTMLPanel;

import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;

public class UserSettingsEditor extends CompositeGwtWidgetElement {

	interface GenralUiBinder extends UiBinder<HTMLPanel, UserSettingsEditor> {
	}

	public static final String TYPE = "bm.ac.UserSettingsEditor";

	private static GenralUiBinder uiBinder = GWT.create(GenralUiBinder.class);

	private net.bluemind.ui.gwtuser.client.UserSettingsEditor settingsWidget;

	private UserSettingsEditor(WidgetElement e) {
		HTMLPanel panel = uiBinder.createAndBindUi(this);
		initWidget(panel);
		settingsWidget = new net.bluemind.ui.gwtuser.client.UserSettingsEditor(e);
		panel.add(settingsWidget);
	}

	@Override
	public void saveModel(JavaScriptObject model) {
		settingsWidget.saveModel(model);
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		settingsWidget.loadModel(model);
	}

	public static void registerType() {
		GwtWidgetElement.register(TYPE, new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

			@Override
			public IGwtWidgetElement create(WidgetElement e) {
				return new UserSettingsEditor(e);
			}
		});
	}

}
