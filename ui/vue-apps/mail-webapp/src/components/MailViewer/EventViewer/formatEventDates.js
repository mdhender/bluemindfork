import i18n from "@bluemind/i18n";
import { DateComparator } from "@bluemind/date";

const isWholeDayEvent = (dtstart, dtend) => dtstart.precision === "Date" && dtend.precision === "Date";
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
    let endDate = new Date(dtend.iso8601);
    if (isWholeDay) {
        endDate.setDate(endDate.getDate() - 1); // Server end date is +1 day for whole days event
    }
    const isOneFullDay = isWholeDay && startDate.getDate() === endDate.getDate();

    const isNotSameDay = [dtend, ...otherRelativeDates]
        .filter(Boolean)
        .some(date => !DateComparator.isSameDay(startDate, new Date(date.iso8601)));
    const formats = [isNotSameDay ? "short_full_date" : undefined, !isWholeDay ? "short_time" : undefined].filter(
        Boolean
    );

    return getFormatedDateRange(startDate, isOneFullDay ? undefined : endDate, formats);
}
