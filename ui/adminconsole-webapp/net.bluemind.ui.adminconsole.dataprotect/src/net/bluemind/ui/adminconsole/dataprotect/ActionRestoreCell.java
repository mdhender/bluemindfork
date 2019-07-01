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
package net.bluemind.ui.adminconsole.dataprotect;

import static com.google.gwt.dom.client.BrowserEvents.CLICK;

import java.util.List;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;

import net.bluemind.ui.adminconsole.dataprotect.l10n.DPTexts;

public class ActionRestoreCell<T> extends AbstractCell<List<ActionHandler<T>>> {

	public ActionRestoreCell() {
		super(BrowserEvents.CLICK);
	}

	@Override
	public void render(com.google.gwt.cell.client.Cell.Context context, List<ActionHandler<T>> value,
			SafeHtmlBuilder sb) {
		sb.appendHtmlConstant("<a href=\"#\">" + DPTexts.INST.restore() + "</a>");
	}

	@Override
	public void onBrowserEvent(com.google.gwt.cell.client.Cell.Context context, Element parent,
			List<ActionHandler<T>> value, NativeEvent event, ValueUpdater<List<ActionHandler<T>>> valueUpdater) {
		super.onBrowserEvent(context, parent, value, event, valueUpdater);
		if (CLICK.equals(event.getType())) {
			event.preventDefault();

			DPRestoreDialog dialog = new DPRestoreDialog();

			for (ActionHandler<T> s : value) {
				dialog.addRestorableOperation(s.getRestoreOp(), s.getCommand());
			}

			dialog.center();
		}
	}

}
