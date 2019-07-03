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
package net.bluemind.ui.admin.client.forms.det;

import static com.google.gwt.dom.client.BrowserEvents.CLICK;

import java.util.HashMap;
import java.util.List;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;

public class ActionsListCell<T> extends AbstractCell<List<ActionHandler<T>>> {

	public ActionsListCell() {
		super(BrowserEvents.CLICK);
	}

	@Override
	public void render(com.google.gwt.cell.client.Cell.Context context, List<ActionHandler<T>> values,
			SafeHtmlBuilder sb) {

		HashMap<String, List<ActionHandler<T>>> actions = new HashMap<String, List<ActionHandler<T>>>();
		for (ActionHandler<T> s : values) {
			// FIXME what the hell ?
			// List<ActionHandler<T>> actionList = actions.get(s.getRestoreOp()
			// .getEntity());
			// if (actionList == null) {
			// actionList = new LinkedList<ActionHandler<T>>();
			// }
			// actionList.add(s);
			// actions.put(s.getRestoreOp().getEntity(), actionList);
		}

		int i = 0;
		for (String entity : actions.keySet()) {
			List<ActionHandler<T>> actionList = actions.get(entity);

			sb.appendHtmlConstant("<ul>");
			for (ActionHandler<T> s : actionList) {
				sb.appendHtmlConstant("<li><a href=\"#\" ahidx=\"" + (i++) + "\">");
				sb.appendEscaped(s.getName());
				sb.appendHtmlConstant("</a></li>");
			}
			sb.appendHtmlConstant("</ul>");
		}
	}

	@Override
	public void onBrowserEvent(Context context, Element parent, List<ActionHandler<T>> value, NativeEvent event,
			ValueUpdater<List<ActionHandler<T>>> valueUpdater) {
		super.onBrowserEvent(context, parent, value, event, valueUpdater);
		if (CLICK.equals(event.getType())) {
			EventTarget eventTarget = event.getEventTarget();
			if (!Element.is(eventTarget)) {
				return;
			}
			Element elem = Element.as(eventTarget);
			String attr = elem.getAttribute("ahidx");
			if (attr != null) {
				int idx = Integer.parseInt(attr);
				ActionHandler<T> handler = value.get(idx);
				handler.handle();
			}

		}
	}
}
