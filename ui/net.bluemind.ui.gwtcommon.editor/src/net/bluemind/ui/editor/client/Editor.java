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
package net.bluemind.ui.editor.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.dom.client.TextAreaElement;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.logical.shared.AttachEvent.Handler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;

public class Editor extends Composite {

	interface EditorUiBinder extends UiBinder<HTMLPanel, Editor> {

	}

	private static final EditorUiBinder binder = GWT.create(EditorUiBinder.class);

	private EditorIds ids;
	private JSEditor editor;

	private Element toolbar;

	public Editor() {
		ids = new EditorIds();
		ids.setToolbar(DOM.createUniqueId());
		ids.setField(DOM.createUniqueId());

		HTMLPanel panel = binder.createAndBindUi(this);
		initWidget(panel);

		toolbar = panel.getElementById(ids.getToolbar());

		Handler handler = new Handler() {

			@Override
			public void onAttachOrDetach(AttachEvent event) {
				if (event.isAttached()) {
					try {
						editor = initEditor(ids.getToolbar(), ids.getField(), GWT.getModuleBaseURL());
					} catch (Throwable t) {
						GWT.log(t.getMessage(), t);
					}
				}
			}
		};
		addAttachHandler(handler);
	}

	public native final JSEditor initEditor(String tb, String ta, String url)
	/*-{
		var editor = new $wnd.bluemind.ui.Editor(ta, tb, {
			ImageDialogPlugin : {
				dataUrlRpc : url + 'dataUrlEncode'
			}
		});
		return editor;
	}-*/;

	public void setText(final String text) {

		Scheduler.get().scheduleDeferred(new ScheduledCommand() {

			@Override
			public void execute() {
				try {
					editor.setValue(text);
				} catch (Throwable t) {
					GWT.log("failed, will try again.");
					// this will loop until we succeed
					setText(text);
				}
			}
		});
	}

	public String getText() {
		return editor.getValue();
	}

	public void resetValue() {
		if (toolbar.getStyle().getDisplay().equals(Display.NONE.getCssName())) {
			setText("-- \n");
		} else {
			setText("-- <br /><br />");
		}
	}

	public void plainEditor() {

		Scheduler.get().scheduleDeferred(new ScheduledCommand() {

			@Override
			public void execute() {
				try {
					editor.textarea();
					editor.setValue("-- \n");
					toolbar.getStyle().setDisplay(Display.NONE);
				} catch (Throwable t) {
					GWT.log("failed, will try again.");
					// this will loop until we succeed
					plainEditor();
				}
			}
		});
	}

	public void htmlEditor() {

		Scheduler.get().scheduleDeferred(new ScheduledCommand() {

			@Override
			public void execute() {
				try {
					editor.setValue("--<br /><br />");
					editor.composer();
					toolbar.getStyle().setDisplay(Display.BLOCK);
				} catch (Throwable t) {
					GWT.log("failed, will try again.");
					// this will loop until we succeed
					htmlEditor();
				}
			}
		});
	}

	private TextAreaElement area() {
		Element wisy = DOM.getElementById(ids.getField());
		TextAreaElement ta = TextAreaElement.as(wisy);
		return ta;
	}

	@UiFactory
	EditorIds getIds() {
		return ids;
	}
}
