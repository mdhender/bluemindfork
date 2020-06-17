// FIXME: move to new store

import { DateComparator } from "@bluemind/date";
import { WeekDay } from "@bluemind/i18n";
import injector from "@bluemind/inject";

export default {
    adapt(event, userName) {
        const main = event.value.main;
        return {
            summary: main.summary,
            organizer: {
                name: main.organizer.commonName,
                mail: main.organizer.mailto
            },
            date: adaptDate(main.dtstart, main.dtend, main.rrule),
            attendees: main.attendees.map(attendee => ({ name: attendee.commonName, mail: attendee.mailto })),
            status: adaptStatus(main.attendees, userName),
            uid: event.uid,
            serverEvent: event
        };
    }
};

function adaptDate(dtstart, dtend, rrule) {
    const vueI18n = injector.getProvider("i18n").get();

    // recurrent event
    if (rrule) {
        return adaptRecurrentEvent(dtstart, dtend, rrule, vueI18n);
    }

    // all day event
    if (dtstart.precision === "Date") {
        return adaptAllDayEvent(dtstart, dtend, vueI18n);
    }

    const startDate = new Date(dtstart.iso8601);
    const endDate = new Date(dtend.iso8601);
    if (DateComparator.isSameDay(startDate, endDate)) {
        // same day, different hours
        return vueI18n.t("common.duration.sameday", {
            date: vueI18n.d(startDate, "full_date_long"),
            startDate: vueI18n.d(startDate, "short_time"),
            endDate: vueI18n.d(endDate, "short_time")
        });
    } else {
        // different days, different hours
        return vueI18n.t("common.duration.weekday", {
            startDate: vueI18n.d(startDate, "full_date_time_long"),
            endDate: vueI18n.d(endDate, "full_date_time_long")
        });
    }
}

function adaptRecurrentEvent(dtstart, dtend, rrule, vueI18n) {
    let res = "";
    const startDate = new Date(dtstart.iso8601);
    const endDate = new Date(dtend.iso8601);
    const startTime = vueI18n.d(startDate, "short_time");
    const endTime = vueI18n.d(endDate, "short_time");

    if (rrule.frequency === "WEEKLY") {
        let days = rrule.byDay.map(o => " " + vueI18n.t("common.day_prefix") + " " + WeekDay.compute(o.day));
        let displayedDays = "";
        if (rrule.byDay.length > 1) {
            const lastDay = days.pop();
            displayedDays += days.join(",") + " " + vueI18n.t("common.and") + " " + lastDay;
        } else {
            displayedDays += days.join(",");
        }
        res =
            dtstart.precision === "DateTime"
                ? vueI18n.tc("common.every_week.with_time", rrule.interval, {
                      count: rrule.interval === 1 ? 0 : rrule.interval,
                      days: displayedDays,
                      startTime,
                      endTime
                  })
                : vueI18n.tc("common.every_week", rrule.interval, {
                      count: rrule.interval === 1 ? 0 : rrule.interval,
                      days: displayedDays
                  });
    } else if (rrule.frequency === "DAILY") {
        res =
            dtstart.precision === "DateTime"
                ? vueI18n.t("common.every_day.with_time", { startTime, endTime })
                : vueI18n.t("common.every_day");
    } else if (rrule.frequency === "MONTHLY") {
        if (rrule.byDay.length === 1) {
            const dayOfMonth = rrule.byDay[0];
            const numberSuffix = vueI18n.tc("common.number_suffix", dayOfMonth.offset, { number: dayOfMonth.offset });
            res =
                dtstart.precision === "DateTime"
                    ? vueI18n.t("common.every_month.same_day.with_time", {
                          number: numberSuffix,
                          day: WeekDay.compute(dayOfMonth.day),
                          startTime,
                          endTime
                      })
                    : vueI18n.t("common.every_month.same_day", {
                          number: numberSuffix,
                          day: WeekDay.compute(dayOfMonth.day)
                      });
        } else {
            res =
                dtstart.precision === "DateTime"
                    ? vueI18n.t("common.every_month.same_date.with_time", {
                          date: startDate.getDate(),
                          startTime,
                          endTime
                      })
                    : vueI18n.t("common.every_month.same_date", { date: startDate.getDate() });
        }
    } else if (rrule.frequency === "YEARLY") {
        res =
            dtstart.precision === "DateTime"
                ? vueI18n.t("common.every_year.day_month.with_time", {
                      dayMonth: vueI18n.d(startDate, "day_month"),
                      startTime,
                      endTime
                  })
                : vueI18n.t("common.every_year.day_month", { dayMonth: vueI18n.d(startDate, "day_month") });
    }
    return res;
}

function adaptAllDayEvent(dtstart, dtend, vueI18n) {
    const startDate = new Date(dtstart.iso8601);
    startDate.setHours(0, 0, 0, 0);

    let endDate = new Date(dtend.iso8601);
    endDate.setHours(0, 0, 0, 0);
    endDate = new Date(endDate.getTime() - 1);

    if (DateComparator.isSameDay(startDate, endDate)) {
        return vueI18n.d(startDate, "full_date_long");
    }
    return vueI18n.t("common.duration.weekday", {
        startDate: vueI18n.d(startDate, "full_date_time_long"),
        endDate: vueI18n.d(endDate, "full_date_time_long")
    });
}

/**
 * return null if user is not in attendees
 */
function adaptStatus(attendees, userName) {
    const userSession = injector.getProvider("UserSession").get();
    const currentAddress = userName + "@" + userSession.domain;
    const user = attendees.find(a => a.mailto === currentAddress);
    return user ? user.partStatus : null;
}
