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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Visibility;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.URL;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.TextBox;

import net.bluemind.calendar.api.gwt.js.JsCalendarDescriptor;
import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;

public class ExternalIcsCalendarCreationWidget extends CompositeGwtWidgetElement {
	public static final String TYPE = "bm.calendar.ExternalIcsCalendarCreation";

	private static ExternalIcsCalendarCreationWidgetUiBinder uiBinder = GWT
			.create(ExternalIcsCalendarCreationWidgetUiBinder.class);

	interface ExternalIcsCalendarCreationWidgetUiBinder extends UiBinder<HTMLPanel, ExternalIcsCalendarCreationWidget> {
	}

	@UiField
	TextBox label;

	@UiField
	TextBox url;

	@UiField
	CheckBox reminder;

	@UiField
	Element spinner;

	@UiField
	Element ok;

	@UiField
	Element notOk;

	private boolean okState;

	private HTMLPanel form;
	private Timer timer = new Timer() {

		@Override
		public void run() {
			doCheckUrl();
		}

	};

	private Request currentRequest;

	public ExternalIcsCalendarCreationWidget() {
		form = uiBinder.createAndBindUi(this);
		initWidget(form);

		url.addValueChangeHandler(new ValueChangeHandler<String>() {

			@Override
			public void onValueChange(ValueChangeEvent<String> event) {
				checkUrl(1000);
			}
		});
		url.addChangeHandler(new ChangeHandler() {

			@Override
			public void onChange(ChangeEvent event) {
				checkUrl(1000);

			}

		});
		setUrlState(false);
	}

	@UiHandler("url")
	void urlChanged(KeyUpEvent e) {
		checkUrl(2000);
	}

	private void doCheckUrl() {
		String urlv = url.getValue();

		if (urlv != null && urlv.length() > 0) {
			if (currentRequest != null) {
				currentRequest.cancel();
				currentRequest = null;
			}
			setUrlState(null);
			RequestBuilder builder = new RequestBuilder(RequestBuilder.GET,
					"calendar/checkIcs?url=" + URL.encodeQueryString(urlv));
			builder.setCallback(new RequestCallback() {

				@Override
				public void onResponseReceived(Request request, Response response) {
					if (response.getStatusCode() == 200) {
						String bodyResp = response.getText();
						if (bodyResp != null && bodyResp.length() > 0) {
							label.setValue(bodyResp);
						}
						String urlReceived = response.getHeader("X-Location");
						url.setValue(urlReceived);
						setUrlState(true);
					} else {
						setUrlState(false, response.getStatusText());
					}

				}

				@Override
				public void onError(Request request, Throwable exception) {
					setUrlState(false, exception.getMessage());
				}
			});
			try {
				currentRequest = builder.send();
			} catch (RequestException e) {
				setUrlState(false, e.getMessage());
			}

		} else {
			setUrlState(false);
		}
	}

	private void checkUrl(int ms) {
		String urlv = url.getValue();
		if (urlv != null && urlv.length() > 0) {
			timer.schedule(ms);
		}
	}

	public void setUrlState(Boolean okState) {
		this.okState = null == okState || !okState ? false : true;
		// addCalendar.setEnabled(false);
		if (okState == null) {
			notOk.getStyle().setVisibility(Visibility.HIDDEN);
			ok.getStyle().setVisibility(Visibility.HIDDEN);
			spinner.getStyle().setVisibility(Visibility.VISIBLE);
		} else if (okState == true) {

			// addCalendar.setEnabled(true);
			notOk.getStyle().setVisibility(Visibility.HIDDEN);
			ok.getStyle().setVisibility(Visibility.VISIBLE);
			spinner.getStyle().setVisibility(Visibility.HIDDEN);
		} else {
			notOk.getStyle().setVisibility(Visibility.VISIBLE);
			ok.getStyle().setVisibility(Visibility.HIDDEN);
			spinner.getStyle().setVisibility(Visibility.HIDDEN);
		}
	}

	public void setUrlState(Boolean okState, String msg) {
		setUrlState(okState);
		notOk.setTitle(msg);
	}

	public static void registerType() {
		GwtWidgetElement.register(TYPE, new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

			@Override
			public IGwtWidgetElement create(WidgetElement el) {
				return new ExternalIcsCalendarCreationWidget();
			}
		});
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		JsCalendarDescriptor descriptor = model.cast();
		label.setText(descriptor.getName());
	}

	@Override
	public void saveModel(JavaScriptObject model) {
		JsCalendarDescriptor descriptor = model.cast();
		if (!okState) {
			return;
		}

		descriptor.setName(label.getText());
		descriptor.getSettings().put("icsUrl", url.getValue());
		descriptor.getSettings().put("syncReminders", reminder.getValue() ? "true" : "false");
		descriptor.getSettings().put("type", "externalIcs");
	}

}
