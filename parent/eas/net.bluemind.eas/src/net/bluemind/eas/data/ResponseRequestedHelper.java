package net.bluemind.eas.data;

import java.util.Date;

import net.bluemind.eas.backend.MSEvent;
import net.bluemind.eas.dto.calendar.CalendarResponse.Recurrence;

public class ResponseRequestedHelper {

	public static boolean isResponseRequested(MSEvent ev) {
		Date now = new Date();
		if (now.before(ev.getStartTime())) {
			return true;
		}
		if (ev.getRecurrence() != null) {
			Recurrence rec = ev.getRecurrence();
			if (rec.until != null && rec.until.before(now)) {
				return false;
			}
			return true;
		}
		return false;
	}
}
