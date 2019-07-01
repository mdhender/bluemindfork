package net.bluemind.core.api.date;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import net.bluemind.core.api.date.BmDateTime.Precision;

public class BmDateTimeHelper {
	public static BmDateTime time(ZonedDateTime dateTime) {
		return time(dateTime, true);
	}

	public static BmDateTime time(ZonedDateTime dateTime, boolean autoDate) {
		if (autoDate && dateTime.getZone().equals(ZoneId.systemDefault()) && dateTime.getHour() == 0
				&& dateTime.getMinute() == 0 && dateTime.getSecond() == 0) {
			long ts = dateTime.toInstant().toEpochMilli();
			;
			return BmDateTimeWrapper.fromTimestamp(ts, dateTime.getZone().getId(), Precision.Date);
		} else {
			return BmDateTimeWrapper.create(dateTime, Precision.DateTime);
		}
	}

	public static BmDateTime time(LocalDateTime dateTime) {
		return time(dateTime, ZoneId.of("UTC"));
	}

	public static BmDateTime time(LocalDateTime dateTime, ZoneId tz) {
		return time(dateTime.atZone(tz));
	}
}
