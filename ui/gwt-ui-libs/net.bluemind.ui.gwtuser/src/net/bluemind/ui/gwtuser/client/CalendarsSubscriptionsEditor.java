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
package net.bluemind.ui.gwtuser.client;

import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.core.container.api.ContainerSubscriptionDescriptor;
import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;
import net.bluemind.ui.gwtuser.client.l10n.BookConstants;
import net.bluemind.ui.gwtuser.client.l10n.CSConstants;

public class CalendarsSubscriptionsEditor extends BaseSubscriptionsEditor {

	public static final String TYPE = "bm.user.CalendarsSubscriptionsEditor";

	public CalendarsSubscriptionsEditor() {
		super(ICalendarUids.TYPE);
	}

	@Override
	protected String getLabel(ContainerSubscriptionDescriptor f, String ownerDisplayName) {
		if (ownerDisplayName != null) {
			return f.name + " " + BookConstants.INST.sharedBy(ownerDisplayName);
		} else {
			return f.name;
		}

	}

	public static void registerType() {
		GwtWidgetElement.register(TYPE, new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

			@Override
			public IGwtWidgetElement create(WidgetElement e) {
				return new CalendarsSubscriptionsEditor();
			}
		});

	}

	@Override
	protected String getAddLabel() {
		return CSConstants.INST.addCalendar();
	}
}
