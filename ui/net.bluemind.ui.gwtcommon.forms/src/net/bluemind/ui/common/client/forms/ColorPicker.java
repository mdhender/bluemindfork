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

import java.util.Arrays;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.Random;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HasValue;

/**
 * @author mehdi
 * 
 */
public class ColorPicker extends Composite implements HasValue<Color> {

	public static interface Resources extends ClientBundle {
		@Source("ColorPicker.css")
		Style style();

	}

	public static interface Style extends CssResource {

		String grid();

		String selected();

		String dot();

		/**
		 * @return
		 */
		String defaultDot();

	}

	private static final List<String> COLORS = Arrays.asList("e7a1a2", "f9ba89", "f7dd8f", "fcfa90", "78d168", "9fdcc9",
			"c6d2b0", "9db7e8", "b5a1e2", "daaec2", "dad9dc", "6b7994", "bfbfbf", "6f6f6f", "4f4f4f", "c11a25",
			"e2620d", "c79930", "b9b300", "368f2b", "329b7a", "778b45", "2858a5", "5c3fa3", "93446b");

	protected static final Resources res = GWT.create(Resources.class);
	protected Style s;
	private Grid grid;
	private ColorDot selected;

	public ColorPicker() {
		this(null);
	}

	public ColorPicker(Color defaultValue) {
		s = res.style();
		s.ensureInjected();
		grid = new Grid(0, 5);
		grid.setStyleName(s.grid());
		initValues(defaultValue);
		initWidget(grid);
	}

	/**
	 * @param defaultValue
	 * 
	 */
	private void initValues(Color defaultValue) {
		int index = 0;
		for (String color : COLORS) {
			addColor(index++, color, defaultValue);
		}
	}

	/**
	 * @param index
	 * @param row
	 * @param color
	 * @param defaultValue
	 * @return
	 */
	private ColorDot addColor(int index, String color, Color defaultValue) {
		Color c = new Color(color);
		return addColor(index, c, defaultValue);
	}

	/**
	 * @param index
	 * @param c
	 * @param defaultValue
	 * @return
	 */
	private ColorDot addColor(int index, Color c, Color defaultValue) {
		int col = index % grid.getColumnCount();
		if (col == 0) {
			grid.insertRow(grid.getRowCount());
		}
		int row = grid.getRowCount() - 1;
		final ColorDot widget = new ColorDot(c);
		if (c.equals(defaultValue)) {
			widget.addStyleName(s.defaultDot());
		} else {
			widget.addStyleName(s.dot());
		}
		widget.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				setSelected(widget, true);
			}
		});
		grid.setWidget(row, col, widget);
		return widget;
	}

	/**
	 * @param widget
	 */
	private void setSelected(final ColorDot widget, boolean fireEvents) {
		Color old = null;
		Color value = null;
		if (selected != null) {
			old = selected.getValue();
			selected.removeStyleName(s.selected());
		}

		if (widget != selected) {
			selected = widget;
			value = selected.getValue();
			selected.addStyleName(s.selected());
		} else {
			selected = null;
		}
		if (fireEvents) {
			ValueChangeEvent.fireIfNotEqual(this, old, value);
		}
	}

	@Override
	public HandlerRegistration addValueChangeHandler(ValueChangeHandler<Color> handler) {
		return addHandler(handler, ValueChangeEvent.getType());
	}

	@Override
	public Color getValue() {
		return selected.getValue();
	}

	@Override
	public void setValue(Color value) {
		setValue(value, false);
	}

	@Override
	public void setValue(Color value, boolean fireEvents) {
		if (selected == null || !selected.getValue().equals(value)) {
			int index = 0;
			int rows = grid.getRowCount();
			int cols = grid.getColumnCount();
			while (index < (rows * cols)) {
				int col = index % cols;
				int row = index / rows;
				final ColorDot widget = (ColorDot) grid.getWidget(row, col);
				if (widget == null) {
					break;
				}
				if (widget.getValue().equals(value)) {
					setSelected(widget, fireEvents);
					return;
				}
				index++;
			}
			final ColorDot widget = addColor(index, value, null);
			setSelected(widget, fireEvents);
		}
	}

	public static Color getRandomColor() {
		int index = Random.nextInt(COLORS.size());
		return new Color(COLORS.get(index));
	}
}
