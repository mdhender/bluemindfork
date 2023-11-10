package net.bluemind.calendar.helper.mail;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import net.bluemind.calendar.VEventUtil;
import net.bluemind.calendar.VEventUtil.EventChanges;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.utils.HeaderUtil;

public class MeetingUpdateDiff {

	public int sequence = 0;
	public Long startDate;
	public Long endDate;
	public String location;
	public boolean majorChange = false;

	private MeetingUpdateDiff() {

	}

	public <T extends VEvent> MeetingUpdateDiff(T oldEvent, T newEvent) {
		this.sequence = newEvent.sequence;
		this.startDate = new BmDateTimeWrapper(oldEvent.dtstart).toDate().getTime();
		this.endDate = new BmDateTimeWrapper(oldEvent.dtend).toDate().getTime();
		this.location = oldEvent.location;
		EnumSet<EventChanges> changes = VEventUtil.eventChanges(oldEvent, newEvent);
		this.majorChange = changes.contains(EventChanges.DTSTART) || changes.contains(EventChanges.DTEND)
				|| changes.contains(EventChanges.RRULE);
	}

	public boolean isUpdate() {
		return sequence != 0;
	}

	public Map<String, String> toMap() {
		Map<String, String> attributes = new HashMap<>();
		attributes.put("seq", String.valueOf(sequence));
		if (startDate != null) {
			attributes.put("std", String.valueOf(startDate));
		}
		if (endDate != null) {
			attributes.put("end", String.valueOf(endDate));
		}
		if (location != null) {
			attributes.put("loc", location);
		}
		attributes.put("major", String.valueOf(majorChange));
		return attributes;
	}

	public static MeetingUpdateDiff fromHeader(HeaderUtil header) {
		MeetingUpdateDiff diff = new MeetingUpdateDiff();
		diff.sequence = header.getHeaderAttribute("seq").map(HeaderUtil.Value::toInteger).orElse(0);
		diff.startDate = header.getHeaderAttribute("std").map(HeaderUtil.Value::toLong).orElse(null);
		diff.endDate = header.getHeaderAttribute("end").map(HeaderUtil.Value::toLong).orElse(null);
		diff.location = header.getHeaderAttribute("loc").map(HeaderUtil.Value::toString).orElse("");
		diff.majorChange = header.getHeaderAttribute("major").map(HeaderUtil.Value::toBoolean).orElse(false);
		return diff;
	}

}
