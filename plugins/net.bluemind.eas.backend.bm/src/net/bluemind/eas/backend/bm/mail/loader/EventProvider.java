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
package net.bluemind.eas.backend.bm.mail.loader;

import net.bluemind.calendar.api.ICalendar;
import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.eas.backend.BackendSession;
import net.bluemind.eas.backend.bm.impl.CoreConnect;

public class EventProvider {

	private static class CoreCoStub extends CoreConnect {

		@Override
		public ICalendar getCalendarService(BackendSession bs, String containerUid) throws ServerFault {
			return super.getCalendarService(bs, containerUid);

		}

	}

	private static final CoreCoStub coreStub = new CoreCoStub();
	private ICalendar cc;
	private BackendSession bs;

	public EventProvider(BackendSession bs) {
		this.bs = bs;
	}

	public ItemValue<VEventSeries> get(String eventUid) {
		cc = coreStub.getCalendarService(bs, ICalendarUids.defaultUserCalendar(bs.getUser().getUid()));
		return cc.getComplete(eventUid);
	}

}
