package net.bluemind.calendar.service.tests;

import net.bluemind.calendar.hook.ICalendarHook;
import net.bluemind.calendar.hook.VEventMessage;

public abstract class CalendarTestHook implements ICalendarHook {

	public enum Action {
		CREATE, UPDATE, DELETE
	}

	@Override
	public void onEventCreated(VEventMessage message) {
		open(Action.CREATE, message);
	}

	@Override
	public void onEventUpdated(VEventMessage message) {
		open(Action.UPDATE, message);
	}

	@Override
	public void onEventDeleted(VEventMessage message) {
		open(Action.DELETE, message);
	}

	protected abstract void open(Action verb, VEventMessage message);

}
