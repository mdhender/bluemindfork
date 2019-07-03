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
package net.bluemind.ui.settings.client;

import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.PopupPanel;

import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;

public class SectionScreen extends Composite {

	private FlowPanel panel = new FlowPanel();
	private WidgetElement widgetElement;
	private DialogBox box;

	public SectionScreen(WidgetElement widgetElement) {
		initWidget(panel);
		this.widgetElement = widgetElement;
		if (isOverlay()) {

			box = new DialogBox();
			box.setGlassEnabled(true);
			box.setGlassStyleName("settingsOverlay");
			box.setModal(true);
			box.setWidget(this);
			box.setAutoHideEnabled(true);
			attach();
			box.addCloseHandler(new CloseHandler<PopupPanel>() {

				@Override
				public void onClose(CloseEvent<PopupPanel> event) {
					if (box.isShowing()) {
						History.back();
					}
				}
			});
		}
	}

	public void show() {
		box.center();
		box.show();
	}

	public void hide() {
		box.hide();
	}

	public void attach() {
		widgetElement.attach(getElement());
	}

	public native boolean isOverlay()
	/*-{
		var over = this.@net.bluemind.ui.settings.client.SectionScreen::widgetElement;
		if (over['overlay']) {
			return over['overlay'];
		} else {
			return false
		}
		;
	}-*/;
}
