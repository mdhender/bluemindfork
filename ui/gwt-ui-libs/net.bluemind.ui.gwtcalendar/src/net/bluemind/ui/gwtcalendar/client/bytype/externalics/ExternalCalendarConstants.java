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
import com.google.gwt.i18n.client.Messages;

public interface ExternalCalendarConstants extends Messages {
	public static final ExternalCalendarConstants INST = GWT.create(ExternalCalendarConstants.class);

	public String launchSync();

	public String lastSync();

	public String icsUrl();

	public String confirmReset();

	public String reset();

	public String emptyLabel();

	public String label();

	public String addCalendar();

	public String syncReminders();

	public String reminders();

	public String syncDeactivated();

	public String syncDeactivatedToolTip();
}
