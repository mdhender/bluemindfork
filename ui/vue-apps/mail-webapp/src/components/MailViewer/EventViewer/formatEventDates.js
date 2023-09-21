import i18n from "@bluemind/i18n";
import { DateComparator } from "@bluemind/date";

const ONE_DAY_IN_MILLISECONDS = 1000 * 60 * 60 * 24;

const isWholeDayEvent = (dtstart, dtend) => dtstart.precision === "Date" && dtend.precision === "Date";
const is24Hours = (start, end) => end.getTime() - start.getTime() === ONE_DAY_IN_MILLISECONDS;
const getFormatedDate = (date, formats) => formats.map(format => i18n.d(date, format)).join(" ");

const getFormatedDateRange = (start, end, formats) => ({
    startDate: getFormatedDate(start, formats),
    endDate: end ? getFormatedDate(end, formats) : undefined
});

export function formatEventDates(dtstart, dtend, otherRelativeDates = []) {
    if (!dtstart || !dtend) {
        return { startDate: "", endDate: "" };
    }

    const isWholeDay = isWholeDayEvent(dtstart, dtend);
    const startDate = new Date(dtstart.iso8601);
    const endDate = new Date(dtend.iso8601);
    const isNotSameDay = [dtend, ...otherRelativeDates]
        .filter(Boolean)
        .some(date => !DateComparator.isSameDay(startDate, new Date(date.iso8601)));
    const formats = [isNotSameDay ? "short_full_date" : undefined, !isWholeDay ? "short_time" : undefined].filter(
        Boolean
    );

    const isOneFullDay = isWholeDay && is24Hours(startDate, endDate);

    return getFormatedDateRange(startDate, isOneFullDay ? undefined : endDate, formats);
}
