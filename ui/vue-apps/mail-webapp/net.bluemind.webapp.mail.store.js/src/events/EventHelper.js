import { DateComparator } from "@bluemind/date";
import injector from "@bluemind/inject";

export default {
    adapt(event) {
        const main = event.value.main;
        return {
            summary: main.summary,
            organizer: {
                name: main.organizer.commonName,
                mail: main.organizer.mailto
            },
            date: adaptDate(main.dtstart, main.dtend),
            attendees: main.attendees.map(attendee => ({ name: attendee.commonName, mail: attendee.mailto }))
        };
    }
};

function adaptDate(dtstart, dtend) {
    const vueI18n = injector.getProvider("i18n").get();

    // all day event
    if (dtstart.precision === "Date") {
        const startDate = new Date(dtstart.iso8601);
        startDate.setHours(0, 0, 0, 0);

        let endDate = new Date(dtend.iso8601);
        endDate.setHours(0, 0, 0, 0);
        endDate = new Date(endDate.getTime() - 1);

        if (DateComparator.isSameDay(startDate, endDate)) {
            return vueI18n.d(startDate, "full_date_long");
        }
        return (
            vueI18n.t("common.from") +
            " " +
            vueI18n.d(startDate, "full_date_long") +
            " " +
            vueI18n.t("common.to.lowercase") +
            " " +
            vueI18n.d(endDate, "full_date_long")
        );
    }

    const startDate = new Date(dtstart.iso8601);
    const endDate = new Date(dtend.iso8601);
    if (DateComparator.isSameDay(startDate, endDate)) {
        // same day, different hours
        return (
            vueI18n.d(startDate, "full_date_long") +
            " " +
            vueI18n.d(startDate, "short_time") +
            " - " +
            vueI18n.d(endDate, "short_time")
        );
    } else {
        // different days, different hours
        return (
            vueI18n.t("common.from") +
            " " +
            vueI18n.d(startDate, "full_date_time") +
            " " +
            vueI18n.t("common.to.lowercase") +
            " " +
            vueI18n.d(endDate, "full_date_time")
        );
    }
}
