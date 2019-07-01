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
package net.bluemind.ui.adminconsole.base.client;

import java.util.Stack;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.i18n.client.Constants;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.FlowPanel;

import net.bluemind.ui.adminconsole.base.Actions;

public class BreadCrumb extends FlowPanel {

	private Stack<ClickHandler> handlers;
	private Stack<Anchor> anchors;
	private Stack<HandlerRegistration> regs;

	interface BreadCrumbConstants extends Constants {
		String centralAdmin();
	}

	interface BCStyle extends CssResource {

		String bcItem();

	}

	interface BCBundle extends ClientBundle {

		@Source("BreadCrumb.css")
		BCStyle getStyle();
	}

	private static final BreadCrumbConstants bcc;
	private static final BCBundle bcb;
	private static final BCStyle style;

	static {
		bcc = GWT.create(BreadCrumbConstants.class);
		bcb = GWT.create(BCBundle.class);
		style = bcb.getStyle();
		style.ensureInjected();
	}

	public BreadCrumb() {
		handlers = new Stack<ClickHandler>();
		anchors = new Stack<Anchor>();
		regs = new Stack<HandlerRegistration>();

		Anchor root = new Anchor(bcc.centralAdmin());
		root.setHref("#root");
		root.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				Actions.get().showWithParams2("root", null);
			}
		});
		root.addStyleName(style.bcItem());
		add(root);
	}

	public void add(Anchor a, ClickHandler cl) {
		if (!handlers.isEmpty()) {
			final ClickHandler toWrap = handlers.peek();
			final Anchor toMakeClickable = anchors.peek();
			ClickHandler wrapper = new ClickHandler() {

				@Override
				public void onClick(ClickEvent event) {
					toWrap.onClick(event);
					// pop items to the left
				}
			};
			toMakeClickable.addClickHandler(wrapper);
		}
		anchors.add(a);
		handlers.add(cl);
		a.addStyleName(style.bcItem());

		add(a);
	}

	public void clearCrumb() {
		for (int i = getWidgetCount() - 1; i > 0; i--) {
			remove(i);
		}
		handlers.clear();
		anchors.clear();
		for (HandlerRegistration hr : regs) {
			hr.removeHandler();
		}
	}

}
