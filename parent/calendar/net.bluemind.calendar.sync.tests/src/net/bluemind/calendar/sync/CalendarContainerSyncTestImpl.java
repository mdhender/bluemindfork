package net.bluemind.calendar.sync;

import java.util.HashMap;

import net.bluemind.core.container.model.Container;
import net.bluemind.core.rest.BmContext;

public class CalendarContainerSyncTestImpl extends CalendarContainerSync {

	private final Exception e;

	public CalendarContainerSyncTestImpl(BmContext context, Container container, Exception e) {
		this.e = e;
		calendarSettings = new HashMap<String, String>();
		calendarSettings.put("icsUrl", "");
	}

	@Override
	protected SyncData fetchData(String modifiedSince, String etag, String md5Hash, String icsUrl, SyncData syncData)
			throws Exception {
		throw e;
	}

}
