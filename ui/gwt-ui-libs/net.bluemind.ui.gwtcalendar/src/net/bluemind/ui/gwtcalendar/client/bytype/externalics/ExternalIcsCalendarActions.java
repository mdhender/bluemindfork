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
package net.bluemind.ui.gwtcalendar.client.bytype.externalics;

import java.util.Date;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

import net.bluemind.calendar.api.ICalendarAsync;
import net.bluemind.calendar.api.gwt.endpoint.CalendarGwtEndpoint;
import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.commons.gwt.JsMapStringString;
import net.bluemind.core.container.api.IContainerSyncAsync;
import net.bluemind.core.container.api.gwt.endpoint.ContainerSyncGwtEndpoint;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.ui.common.client.forms.Ajax;
import net.bluemind.ui.gwttask.client.TaskWatcher;

public class ExternalIcsCalendarActions extends CompositeGwtWidgetElement {

	public static final String TYPE = "bm.calendar.ExternalIcsCalendarActions";
	private String containerUid;
	private Anchor syncAnchor;
	private Label icsUrl;
	private Label syncDeactivated;
	private Label nextSync;
	private Label status;

	public ExternalIcsCalendarActions() {

		HorizontalPanel content = new HorizontalPanel();
		VerticalPanel vp = new VerticalPanel();
		icsUrl = new Label();
		vp.add(icsUrl);

		nextSync = new Label();
		vp.add(nextSync);

		HorizontalPanel hp = new HorizontalPanel();

		Anchor reset = new Anchor(ExternalCalendarConstants.INST.reset());
		reset.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				doReset();
			}

		});

		syncAnchor = new Anchor(ExternalCalendarConstants.INST.launchSync());
		syncAnchor.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				launchSync();
			}

		});

		hp.add(reset);
		hp.add(syncAnchor);

		syncDeactivated = new Label();
		syncDeactivated.setStyleName("fa fa-lg fa-warning");
		syncDeactivated.setTitle(ExternalCalendarConstants.INST.syncDeactivatedToolTip());
		syncDeactivated.getElement().getStyle().setProperty("marginLeft", "10px");
		syncDeactivated.getElement().getStyle().setProperty("color", "red");
		syncDeactivated.setVisible(false);
		hp.add(syncDeactivated);

		vp.add(hp);

		status = new Label();

		status.getElement().getStyle().setProperty("marginLeft", "40px");
		status.getElement().getStyle().setProperty("marginTop", "18px");
		status.setVisible(false);

		HorizontalPanel hp2 = new HorizontalPanel();
		hp2.add(status);

		content.add(vp);
		content.add(hp2);

		initWidget(content);

		syncAnchor.getElement().getStyle().setPaddingLeft(10, Unit.PX);
	}

	protected void doReset() {
		if (Window.confirm(ExternalCalendarConstants.INST.confirmReset())) {
			ICalendarAsync cal = new CalendarGwtEndpoint(Ajax.TOKEN.getSessionId(), containerUid);
			cal.reset(new DefaultAsyncHandler<TaskRef>() {
				@Override
				public void success(TaskRef value) {
					TaskWatcher.track(value.id);
				}
			});
		}
	}

	@Override
	public void attach(Element parent) {
		super.attach(parent);

		IContainerSyncAsync sync = new ContainerSyncGwtEndpoint(Ajax.TOKEN.getSessionId(), containerUid);
		sync.getLastSync(new DefaultAsyncHandler<Date>() {

			@Override
			public void success(Date value) {
				if (value != null) {
					syncAnchor.setTitle(ExternalCalendarConstants.INST.lastSync() + " " + value);
				}
			}
		});
	}

	protected void launchSync() {

		IContainerSyncAsync sync = new ContainerSyncGwtEndpoint(Ajax.TOKEN.getSessionId(), containerUid);
		sync.sync(new AsyncHandler<TaskRef>() {

			@Override
			public void success(TaskRef value) {
				TaskWatcher.track(value.id, false).thenAccept(v -> {
					checkAutoSync();
					loadNextSync();
					loadSyncStatus();
				});
			}

			@Override
			public void failure(Throwable e) {

			}

		});
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		super.loadModel(model);
		JsMapStringString m = model.cast();
		containerUid = m.get("container");
		icsUrl.setText(m.get("icsUrl"));
		this.loadNextSync();
		this.checkAutoSync();
		this.loadSyncStatus();
	}

	private void checkAutoSync() {
		final ICalendarAsync calendarService = new CalendarGwtEndpoint(Ajax.TOKEN.getSessionId(), containerUid);
		calendarService.isAutoSyncActivated(new AsyncHandler<Boolean>() {

			@Override
			public void success(Boolean autoSyncActivate) {
				syncDeactivated.setVisible(!autoSyncActivate);
			}

			@Override
			public void failure(Throwable e) {
			}
		});
	}

	private void loadNextSync() {
		final IContainerSyncAsync syncService = new ContainerSyncGwtEndpoint(Ajax.TOKEN.getSessionId(), containerUid);
		syncService.getNextSync(new AsyncHandler<Date>() {

			@Override
			public void success(Date date) {
				nextSync.setText(ExternalCalendarConstants.INST.nextSync() + ": "
						+ DateTimeFormat.getFormat(PredefinedFormat.DATE_TIME_MEDIUM).format(date));
			}

			@Override
			public void failure(Throwable e) {
			}

		});
	}

	private void loadSyncStatus() {
		final IContainerSyncAsync syncService = new ContainerSyncGwtEndpoint(Ajax.TOKEN.getSessionId(), containerUid);
		syncService.getSyncStatus(new AsyncHandler<String>() {

			@Override
			public void success(String value) {
				if (value != null && !value.isEmpty()) {
					String code = value.substring(0, value.indexOf(":"));
					switch (code) {
					case "OK":
						status.setStyleName("fa fa-lg fa-ok");
						break;
					case "NSD": // no sync has been done
						status.setStyleName("fa fa-lg fa-ok");
						break;
					case "NM": // no elements have been modified
						status.setStyleName("fa fa-lg fa-ok");
						break;
					case "OKM": // OK, but many changes
						status.setStyleName("fa fa-lg fa-warning");
						break;
					case "ESE": // external server exception
						status.getElement().getStyle().setProperty("color", "red");
						status.setStyleName("fa fa-lg fa-warning");
						break;
					case "AUTH": // http auth error
						status.getElement().getStyle().setProperty("color", "red");
						status.setStyleName("fa fa-lg fa-warning");
						break;
					case "USE": // unknown external server exception
						status.getElement().getStyle().setProperty("color", "red");
						status.setStyleName("fa fa-lg fa-warning");
						break;
					case "ISE": // unknown internal server exception
						status.getElement().getStyle().setProperty("color", "red");
						status.setStyleName("fa fa-lg fa-warning");
						break;
					}
				}

			}

			@Override
			public void failure(Throwable e) {
			}

		});

	}

	public void setModel(String containerUid) {
		this.containerUid = containerUid;
	}

	public static void registerType() {

		GwtWidgetElement.register(TYPE, new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

			@Override
			public IGwtWidgetElement create(WidgetElement el) {
				return new ExternalIcsCalendarActions();
			}
		});
	}

}
