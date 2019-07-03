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
package net.bluemind.ui.gwttag.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.TextBox;

import net.bluemind.tag.api.Tag;
import net.bluemind.ui.common.client.forms.Color;
import net.bluemind.ui.common.client.forms.ColorBox;
import net.bluemind.ui.gwttag.client.l10n.TagManagementConstants;

/**
 * @author mehdi
 * 
 */
public class TagInlineEditor extends Composite implements HasValue<Tag> {

	public static interface Resources extends ClientBundle {
		@Source("TagInlineEditor.css")
		Style editStyle();

	}

	public static interface Style extends CssResource {

		String inedit();

		String container();
	}

	protected static final Resources res = GWT.create(Resources.class);
	protected Style s;
	protected HorizontalPanel container;
	protected TextBox label;
	protected ColorBox color;
	private Tag value;
	private boolean readOnly;

	public TagInlineEditor() {
		this(null);
	}

	public TagInlineEditor(Tag tag) {
		this(tag, false);
	}

	public TagInlineEditor(Tag tag, boolean readOnly) {
		value = tag;
		this.readOnly = readOnly;
		createEditor();
		initWidget(container);
	}

	public void createEditor() {

		s = res.editStyle();
		s.ensureInjected();

		container = new HorizontalPanel();
		container.setStyleName(s.container());
		container.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);

		label = new TextBox();
		label.setEnabled(!readOnly);
		if (value != null) {
			label.setValue(value.label);
			if (readOnly) {
				// FIXME
				color = new ColorBox(new Color(value.color), null);
				color.setReadOnly(readOnly);
			} else {
				color = new ColorBox(new Color(value.color));
			}
		} else {
			color = new ColorBox();
		}

		color.setTitle(TagManagementConstants.INST.color());

		container.add(color);
		container.add(label);

		label.addFocusHandler(new FocusHandler() {
			@Override
			public void onFocus(FocusEvent event) {
				container.addStyleName(s.inedit());

			}
		});

		label.addBlurHandler(new BlurHandler() {
			@Override
			public void onBlur(BlurEvent event) {
				save(true);
			}
		});

		color.addValueChangeHandler(new ValueChangeHandler<Color>() {
			@Override
			public void onValueChange(ValueChangeEvent<Color> event) {
				save(true);
			}
		});

		label.addKeyDownHandler(new KeyDownHandler() {
			@Override
			public void onKeyDown(KeyDownEvent event) {
				if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
					save(true);
				}
			}
		});

	}

	public void setValue(Tag tag) {
		setValue(tag, false);
	}

	@Override
	public void setValue(Tag value, boolean fireEvents) {
		Tag old = this.value;
		this.value = value;
		reset();
		if (fireEvents) {
			ValueChangeEvent.fireIfNotEqual(this, old, value);
		}
	}

	public Tag getValue() {
		return value;
	}

	protected void reset() {
		label.setValue(value.label);
		if (readOnly) {
			color.setValue(new Color(value.color));
		} else {
			// FIXME
			color.setValue(new Color(value.color));
			// value.getDefaultColor());
		}
		container.removeStyleName(s.inedit());
	}

	protected void save(boolean fireEvents) {
		container.removeStyleName(s.inedit());
		boolean hasChanged = false;
		if (readOnly) {
			if (!value.color.equals(color.getValue().getRGB())) {
				value.color = color.getValue().getRGB();
				hasChanged = true;
			}
		} else {
			if (!value.color.equals(color.getValue().getRGB())) {
				value.color = color.getValue().getRGB();
				hasChanged = true;
			}
			if (!value.label.equals(label.getValue())) {
				value.label = label.getValue();
				hasChanged = true;
			}
		}
		if (fireEvents && hasChanged) {
			ValueChangeEvent.fire(this, value);
		}

	}

	@Override
	public HandlerRegistration addValueChangeHandler(ValueChangeHandler<Tag> handler) {
		return addHandler(handler, ValueChangeEvent.getType());
	}

}
