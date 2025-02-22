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
package net.bluemind.calendar.service.http.tests;

import net.bluemind.calendar.api.ICalendarView;
import net.bluemind.calendar.service.tests.CalendarViewServiceTests;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.http.ClientSideServiceProvider;

public class HttpCalendarViewServiceTests extends CalendarViewServiceTests {

	@Override
	protected ICalendarView getCalendarViewService(SecurityContext context, Container container) throws ServerFault {
		return ClientSideServiceProvider.getProvider("http://127.0.0.1:8090", context.getSessionId())
				.instance(ICalendarView.class, container.uid);
	}

}
