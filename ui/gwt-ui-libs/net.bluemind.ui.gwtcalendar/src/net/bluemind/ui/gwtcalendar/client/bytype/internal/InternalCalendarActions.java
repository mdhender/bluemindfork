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
package net.bluemind.ui.gwtcalendar.client.bytype.internal;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.HorizontalPanel;

import net.bluemind.calendar.api.ICalendarAsync;
import net.bluemind.calendar.api.gwt.endpoint.CalendarGwtEndpoint;
import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.commons.gwt.JsMapStringString;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.gwtconsoleapp.base.notification.Notification;
import net.bluemind.ui.common.client.OverlayScreen;
import net.bluemind.ui.common.client.SizeHint;
import net.bluemind.ui.common.client.forms.Ajax;
import net.bluemind.ui.gwtcalendar.client.icsimport.ICSUploadConstants;
import net.bluemind.ui.gwtcalendar.client.icsimport.ICSUploadDialog;
import net.bluemind.ui.gwttask.client.TaskWatcher;

public class InternalCalendarActions extends CompositeGwtWidgetElement {

	public static final String TYPE = "bm.calendar.InternalCalendarActions";

	private String containerUid;

	public InternalCalendarActions() {

		HorizontalPanel panel = new HorizontalPanel();

		Anchor reset = new Anchor(InternalCalendarConstants.INST.reset());
		reset.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				doReset();
			}

		});

		Anchor importIcs = new Anchor(ICSUploadConstants.INST.importICSBtn());
		importIcs.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				showUpload();
			}

		});

		panel.add(reset);
		panel.add(importIcs);
		initWidget(panel);

		importIcs.getElement().getStyle().setPaddingLeft(10, Unit.PX);
	}

	protected void doReset() {
		if (Window.confirm(InternalCalendarConstants.INST.confirmReset())) {
			ICalendarAsync cal = new CalendarGwtEndpoint(Ajax.TOKEN.getSessionId(), containerUid);
			cal.reset(new DefaultAsyncHandler<TaskRef>() {
				@Override
				public void success(TaskRef value) {
					TaskWatcher.track(value.id);
				}
			});
		}
	}

	protected void showUpload() {

		ICSUploadDialog vud = new ICSUploadDialog(containerUid);
		SizeHint sh = vud.getSizeHint();
		final OverlayScreen os = new OverlayScreen(vud, sh.getWidth(), sh.getHeight());
		vud.setOverlay(os);
		os.center();

	}

	@Override
	public void loadModel(JavaScriptObject model) {
		super.loadModel(model);
		JsMapStringString m = model.cast();
		containerUid = m.get("container");
	}

	public void setModel(String containerUid) {
		this.containerUid = containerUid;
	}

	public static void registerType() {

		GwtWidgetElement.register(TYPE, new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

			@Override
			public IGwtWidgetElement create(WidgetElement el) {
				return new InternalCalendarActions();
			}
		});
	}

}
