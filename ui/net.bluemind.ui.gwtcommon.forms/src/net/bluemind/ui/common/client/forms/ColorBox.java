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
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.PopupPanel;

/**
 * @author mehdi
 * 
 */
public class ColorBox extends Composite implements HasValue<Color> {

	public static interface Resources extends ClientBundle {
		@Source("ColorBox.css")
		Style style();

	}

	public static interface Style extends CssResource {
		String box();

	}

	private class ColorBoxHandler implements ValueChangeHandler<Color>, ClickHandler, CloseHandler<PopupPanel> {

		public void onClick(ClickEvent event) {
			if (readOnly) {
				return;
			}
			if (isPickerShowing()) {
				hidePicker();
			} else {
				showPicker();
			}
		}

		public void onClose(CloseEvent<PopupPanel> event) {
		}

		public void onValueChange(ValueChangeEvent<Color> event) {
			if (event.getValue() != null || acceptNull) {
				setValue(event.getValue(), true);
			} else {
				picker.setValue(dot.getValue(), false);
			}
			hidePicker();
		}
	}

	private final PopupPanel popup;
	private final ColorDot dot;
	private final ColorPicker picker;
	protected static final Resources res = GWT.create(Resources.class);
	protected Style s;
	protected Color defaultValue;
	protected boolean acceptNull;
	private boolean readOnly;

	public ColorBox() {
		this(null, null, false);
	}

	public ColorBox(Color color) {
		this(color, null, false);
	}

	public ColorBox(Color color, Color defaultValue) {
		this(color, defaultValue, false);
	}

	public ColorBox(Color color, boolean acceptNull) {
		this(color, null, acceptNull);
	}

	private ColorBox(Color color, Color defaultValue, boolean acceptNull) {

		this.defaultValue = defaultValue;
		this.acceptNull = acceptNull;
		res.style().ensureInjected();

		dot = new ColorDot();
		dot.addStyleName(res.style().box());

		this.picker = new ColorPicker(defaultValue);
		this.popup = new PopupPanel(true);

		popup.addAutoHidePartner(dot.getElement());
		popup.setWidget(picker);

		initWidget(dot);

		ColorBoxHandler handler = new ColorBoxHandler();
		picker.addValueChangeHandler(handler);
		dot.addClickHandler(handler);
		dot.setDirectionEstimator(false);
		popup.addCloseHandler(handler);

		setValue(color);
	}

	public HandlerRegistration addValueChangeHandler(ValueChangeHandler<Color> handler) {
		return addHandler(handler, ValueChangeEvent.getType());
	}

	/**
	 * Get text dot.
	 * 
	 * @return the text dot used to enter the formatted date
	 */
	public ColorDot getTextBox() {
		return dot;
	}

	public Color getValue() {
		return dot.getValue();
	}

	public void hidePicker() {
		popup.hide();
	}

	public boolean isPickerShowing() {
		return popup.isShowing();
	}

	public void setValue(Color color) {
		setValue(color, false);
	}

	public void setValue(Color color, boolean fireEvents) {
		if (color == null && !acceptNull) {
			if (defaultValue != null) {
				color = defaultValue;
			} else {
				color = ColorPicker.getRandomColor();
			}
		}
		Color old = dot.getValue();
		picker.setValue(color, false);
		dot.setColor(color);

		if (fireEvents) {
			ValueChangeEvent.fireIfNotEqual(this, old, color);
		}
	}

	public void showPicker() {
		popup.showRelativeTo(this);
	}

	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
		if (isPickerShowing()) {
			hidePicker();
		}
	}

}
