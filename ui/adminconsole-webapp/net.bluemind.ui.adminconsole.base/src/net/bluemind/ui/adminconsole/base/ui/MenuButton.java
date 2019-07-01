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
package net.bluemind.ui.adminconsole.base.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.ToggleButton;

public abstract class MenuButton extends ToggleButton {

	public enum PopupOrientation {
		UpRight, UpLeft, DownRight, DownLeft
	}

	protected PopupPanel pp;
	protected boolean mouseInPopup;
	private final PopupOrientation popupOrientation;

	public MenuButton(String text, PopupOrientation popupOrientation) {
		super(text);
		this.popupOrientation = popupOrientation;
		setStyleName("button");
		createPopup();
	}

	private void createPopup() {
		pp = new PopupPanel() {
			@Override
			public void onBrowserEvent(Event event) {
				Element related = event.getRelatedEventTarget().cast();
				switch (DOM.eventGetType(event)) {
				case Event.ONMOUSEOVER:
					if (related != null && getElement().isOrHasChild(related)) {
						return;
					}
					GWT.log("onMouseOver", null);
					mouseInPopup = true;
					break;
				case Event.ONMOUSEOUT:
					if (related != null && getElement().isOrHasChild(related)) {
						return;
					}
					GWT.log("onMouseOut", null);
					mouseInPopup = false;
					break;
				}
				DomEvent.fireNativeEvent(event, this, this.getElement());
			}
		};
		pp.sinkEvents(Event.ONMOUSEOUT | Event.ONMOUSEOVER);

		addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				if (pp.isShowing()) {
					pp.hide();
					setDown(false);
				} else {
					pp.show();
					Element re = event.getRelativeElement();
					pp.setPopupPosition(computeXPosition(re), computeYPosition(re));
				}
			}
		});

		addBlurHandler(new BlurHandler() {
			@Override
			public void onBlur(BlurEvent event) {
				GWT.log("onBlur: inPopup" + mouseInPopup + " jsShowing: " + pp.isShowing(), null);
				if (pp.isShowing() && !mouseInPopup) {
					pp.hide();
					setDown(false);
				}
			}
		});
	}

	private int computeXPosition(Element reference) {
		switch (popupOrientation) {
		case DownLeft:
		case UpLeft:
			return reference.getAbsoluteLeft() - pp.getElement().getOffsetWidth() + reference.getOffsetWidth();
		case DownRight:
		case UpRight:
			return reference.getAbsoluteLeft();
		}
		throw new RuntimeException("unexpected value");
	}

	private int computeYPosition(Element reference) {
		switch (popupOrientation) {
		case UpLeft:
		case UpRight:
			return reference.getAbsoluteTop() - pp.getElement().getOffsetHeight();
		case DownLeft:
		case DownRight:
			return reference.getAbsoluteTop() + reference.getOffsetHeight();
		}
		throw new RuntimeException("unexpected value");
	}
}
