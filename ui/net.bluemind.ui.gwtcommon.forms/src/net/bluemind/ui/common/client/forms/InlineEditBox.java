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
package net.bluemind.ui.common.client.forms;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;

/**
 * @author mehdi
 * 
 */
public class InlineEditBox extends Composite {

	public static interface Resources extends ClientBundle {
		@Source("InlineEditBox.css")
		Style editStyle();

	}

	public static interface Style extends CssResource {

		String inedit();

		String container();
	}

	protected static final Resources res = GWT.create(Resources.class);
	protected Style s;
	protected FlowPanel container;
	protected TextBox textBox;
	private String value;
	private ScheduledCommand action;

	public InlineEditBox() {
		this("", null);
	}

	public InlineEditBox(String label) {
		this(label, null);
	}

	public InlineEditBox(String label, final ScheduledCommand action) {
		createTextBox();
		setValue(label);
		initWidget(container);
		this.action = action;
	}

	/**
	 * @param action
	 */
	public void setAction(final ScheduledCommand action) {
		this.action = action;
	}

	public void setValue(String v) {
		value = v;
		textBox.setValue(value);
	}

	public String getValue() {
		return value;
	}

	public void createTextBox() {

		s = res.editStyle();
		s.ensureInjected();

		container = new FlowPanel();
		container.setStyleName(s.container());

		textBox = new TextBox();
		container.add(textBox);

		final Label validate = new Label();
		validate.setStyleName("fa fa-lg fa-check");
		container.add(validate);
		validate.getElement().getStyle().setCursor(Cursor.POINTER);
		validate.getElement().getStyle().setMarginRight(10, Unit.PX);
		validate.setVisible(false);

		final Label cancel = new Label();
		cancel.setStyleName("fa fa-lg fa-times");
		container.add(cancel);
		cancel.getElement().getStyle().setCursor(Cursor.POINTER);
		cancel.setVisible(false);

		textBox.addKeyDownHandler(new KeyDownHandler() {
			@Override
			public void onKeyDown(KeyDownEvent event) {
				if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
					if (action != null) {
						Scheduler.get().scheduleDeferred(action);
					}
					value = textBox.getValue();
					container.removeStyleName(s.inedit());
				} else if (event.getNativeKeyCode() == KeyCodes.KEY_ESCAPE) {
					textBox.setValue(value);
					container.removeStyleName(s.inedit());
				}
			}
		});

		validate.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				if (action != null) {
					Scheduler.get().scheduleDeferred(action);
				}
				value = textBox.getValue();
				container.removeStyleName(s.inedit());
			}
		});

		cancel.addMouseDownHandler(new MouseDownHandler() {

			@Override
			public void onMouseDown(MouseDownEvent event) {
				textBox.setValue(value);
				container.removeStyleName(s.inedit());
			}
		});

		textBox.addFocusHandler(new FocusHandler() {

			@Override
			public void onFocus(FocusEvent event) {
				container.addStyleName(s.inedit());
				validate.setVisible(true);
				cancel.setVisible(true);

			}
		});

		textBox.addBlurHandler(new BlurHandler() {

			@Override
			public void onBlur(BlurEvent event) {
				container.removeStyleName(s.inedit());
				validate.setVisible(false);
				cancel.setVisible(false);
				if (!textBox.getValue().equals(value)) {
					if (action != null) {
						Scheduler.get().scheduleDeferred(action);
					}
					value = textBox.getValue();
				}
			}
		});
	}

}
