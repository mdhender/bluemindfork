import { loadingStatus } from "@bluemind/mail";
import { DateComparator, WeekDayCodes } from "@bluemind/date";
import { WeekDay } from "@bluemind/i18n";
import injector from "@bluemind/inject";
import { sanitizeHtml } from "@bluemind/html-utils";

const { LoadingStatus } = loadingStatus;

export default {
    adapt(event, mailboxOwner, originator, recuridIsoDate) {
        const infos = this.eventInfos(event, recuridIsoDate);
        return {
            summary: infos.summary,
            organizer: {
                name: infos.organizer.commonName,
                mail: infos.organizer.mailto
            },
            date: adaptDate(infos.dtstart, infos.dtend, infos.rrule),
            conference: infos.conference,
            attendees: infos.attendees.map(attendee => ({ name: attendee.commonName, mail: attendee.mailto })),
            mailboxOwner,
            status: this.findAttendee(infos.attendees, mailboxOwner)?.partStatus,
            recuridIsoDate,
            uid: event.uid,
            serverEvent: event,
            sanitizedDescription: infos.description ? sanitizeHtml(infos.description) : undefined,
            counter: adaptCounter(event, originator, recuridIsoDate),
            loading: LoadingStatus.LOADED
        };
    },

    applyCounter(adaptedEvent, originator, recuridIsoDate) {
        const event = adaptedEvent.serverEvent;
        const counter = event.value.counters.find(c => matchCounter(c, originator, recuridIsoDate)).counter;

        // the counter may apply to:
        // - [CASE A] the whole series / a single event
        // - [CASE B] one of the existing exceptions of a recurrent event
        // - [CASE C] one of the "standard" occurrences of a recurrent event (does not exist yet as an exception)
        let eventToModify;
        const applyToMainEvent = !counter.recurid;
        if (applyToMainEvent) {
            // [CASE A]
            eventToModify = event.value.main;
        } else {
            // [CASE B]
            eventToModify = event.value.occurrences.find(o => o.recurid.iso8601 === counter.recurid.iso8601);
            if (!eventToModify) {
                // [CASE C]
                eventToModify = adaptedEvent.counter.occurrence;
                event.value.occurrences.push(eventToModify);
            }
        }

        // modify dates
        eventToModify.dtstart = counter.dtstart;
        eventToModify.dtend = counter.dtend;
        eventToModify.rrule = counter.rrule;

        // remove applied counter and reset obsolete data
        if (applyToMainEvent) {
            event.value.counters = [];
            event.value.occurrences = [];
            eventToModify.exdate = null;
        } else {
            const allCountersForThisOccurrence = event.value.counters
                .filter(c => c.counter.recurid.iso8601 === counter.recurid.iso8601)
                .map((c, index) => index);
            allCountersForThisOccurrence.forEach(c => event.value.counters.splice(c, 1));
        }
        resetParticipationStatuses(adaptedEvent.serverEvent, recuridIsoDate);
    },

    removeCounter(adaptedEvent, originator, recuridIsoDate) {
        const counterIndex = adaptedEvent.serverEvent.value.counters.findIndex(c =>
            matchCounter(c, originator, recuridIsoDate)
        );
        adaptedEvent.serverEvent.value.counters.splice(counterIndex, 1);
    },

    setStatus(adaptedEvent, status) {
        const infos = this.eventInfos(adaptedEvent.serverEvent, adaptedEvent.recuridIsoDate);
        this.findAttendee(infos.attendees, adaptedEvent.mailboxOwner).partStatus = status;
    },

    eventInfos(event, recuridIsoDate) {
        return !recuridIsoDate
            ? event.value.main
            : event.value.occurrences.find(occurrence => occurrence.recurid.iso8601 === recuridIsoDate);
    },

    findAttendee(attendees, mailboxOwner) {
        return attendees.find(a => a.dir && a.dir.split("/").pop() === mailboxOwner);
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
    const startDate = new Date(dtstart.iso8601);
    const endDate = new Date(dtend.iso8601);
    const params = {
        dtstart,
        dtend,
        rrule,
        vueI18n,
        startDate,
        endDate,
        startTime: vueI18n.d(startDate, "short_time"),
        endTime: vueI18n.d(endDate, "short_time"),
        count: rrule.interval ? rrule.interval : 1,
        startMonth: vueI18n.d(startDate, "month"),
        startYear: startDate.getFullYear()
    };

    switch (rrule.frequency) {
        case "WEEKLY":
            return adaptWeeklyRecurrentEvent(params);
        case "DAILY":
            return adaptDailyRecurrentEvent(params);
        case "MONTHLY":
            return adaptMonthlyRecurrentEvent(params);
        case "YEARLY":
            return adaptYearlyRecurrentEvent(params);
        default:
            return "";
    }
}

function adaptWeeklyRecurrentEvent({
    dtstart,
    rrule,
    vueI18n,
    startDate,
    startTime,
    endTime,
    count,
    startMonth,
    startYear
}) {
    let days = rrule.byDay
        .sort((a, b) => WeekDayCodes.indexOf(a.day) - WeekDayCodes.indexOf(b.day))
        .map(o => " " + vueI18n.t("common.day_prefix") + " " + WeekDay.compute(o.day));
    let displayedDays = "";
    if (rrule.byDay.length > 1) {
        const lastDay = days.pop();
        displayedDays += days.join(",") + " " + vueI18n.t("common.and") + " " + lastDay;
    } else {
        displayedDays += days.join(",");
    }
    const dayMonth = startDate.getDate();
    return dtstart.precision === "DateTime"
        ? vueI18n.tc("common.every_week.with_time", count, {
              count,
              days: displayedDays,
              startTime,
              endTime,
              dayMonth,
              startMonth,
              startYear
          })
        : vueI18n.tc("common.every_week", count, {
              count,
              days: displayedDays,
              dayMonth,
              startMonth,
              startYear
          });
}

function adaptDailyRecurrentEvent({ dtstart, vueI18n, startDate, startTime, endTime, count, startMonth, startYear }) {
    const dayMonth = startDate.getDate();
    return dtstart.precision === "DateTime"
        ? vueI18n.tc("common.every_day.with_time", count, {
              startTime,
              endTime,
              count,
              dayMonth,
              startMonth,
              startYear
          })
        : vueI18n.tc("common.every_day", count, { count, dayMonth, startMonth, startYear });
}

function adaptMonthlyRecurrentEvent({
    dtstart,
    rrule,
    vueI18n,
    startDate,
    startTime,
    endTime,
    count,
    startMonth,
    startYear
}) {
    if (rrule.byDay && rrule.byDay.length === 1) {
        const dayOfMonth = rrule.byDay[0];
        const numberSuffix = vueI18n.tc("common.number_suffix", dayOfMonth.offset, { number: dayOfMonth.offset });
        return dtstart.precision === "DateTime"
            ? vueI18n.tc("common.every_month.same_day.with_time", count, {
                  number: numberSuffix,
                  day: WeekDay.compute(dayOfMonth.day),
                  startTime,
                  endTime,
                  count,
                  startMonth,
                  startYear
              })
            : vueI18n.tc("common.every_month.same_day", count, {
                  number: numberSuffix,
                  day: WeekDay.compute(dayOfMonth.day),
                  count,
                  startMonth,
                  startYear
              });
    } else {
        return dtstart.precision === "DateTime"
            ? vueI18n.tc("common.every_month.same_date.with_time", count, {
                  date: startDate.getDate(),
                  startTime,
                  endTime,
                  count,
                  startMonth,
                  startYear
              })
            : vueI18n.tc("common.every_month.same_date", count, {
                  date: startDate.getDate(),
                  count,
                  startMonth,
                  startYear
              });
    }
}

function adaptYearlyRecurrentEvent({ dtstart, vueI18n, startDate, startTime, endTime, count, startYear }) {
    return dtstart.precision === "DateTime"
        ? vueI18n.tc("common.every_year.day_month.with_time", count, {
              dayMonth: vueI18n.d(startDate, "day_month"),
              startTime,
              endTime,
              count,
              startYear
          })
        : vueI18n.tc("common.every_year.day_month", count, {
              dayMonth: vueI18n.d(startDate, "day_month"),
              count,
              startYear
          });
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

function adaptCounter(event, originator, recuridIsoDate) {
    if (event.value.counters) {
        const counter = event.value.counters.find(c => matchCounter(c, originator, recuridIsoDate));
        if (counter) {
            adjustUntil(counter.counter);
            const occurrence =
                counter.counter.recurid &&
                (event.value.occurrences.find(o => o.recurid.iso8601 === counter.counter.recurid.iso8601) ||
                    buildOccurrence(event, counter.counter.recurid));
            const initial = occurrence || event.value.main;
            adjustByDay(counter.counter, initial);
            return {
                initialDate: adaptDate(initial.dtstart, initial.dtend, initial.rrule),
                proposedDate: adaptDate(counter.counter.dtstart, counter.counter.dtend, counter.counter.rrule),
                originator,
                status: counter.counter.attendees.find(a => a.mailto === originator).partStatus,
                occurrence
            };
        }
    }
}

/** Make sure rrule.byday is coherent with dtstart. */
function adjustByDay(counterEvent, initialEvent) {
    if (counterEvent.rrule && counterEvent.rrule.byDay && counterEvent.rrule.byDay.length) {
        const start = new Date(counterEvent.dtstart.iso8601);
        const initialStart = new Date(initialEvent.dtstart.iso8601);
        if (!DateComparator.isSameDay(start, initialStart)) {
            if (counterEvent.rrule.frequency === "WEEKLY") {
                counterEvent.rrule.byDay.push({ day: WeekDayCodes[start.getDay()], offset: 0 });
                const toBeRemovedIndex = counterEvent.rrule.byDay.findIndex(
                    ({ day }) => day === WeekDayCodes[initialStart.getDay()]
                );
                if (toBeRemovedIndex !== -1) {
                    counterEvent.rrule.byDay.splice(toBeRemovedIndex, 1);
                }
            } else if (["MONTHLY", "YEARLY"].includes(counterEvent.rrule.frequency)) {
                counterEvent.rrule.byday = [];
                let pos = Math.ceil(start.getDate() / 7);
                if (pos === 5) {
                    pos = -1;
                }
                counterEvent.rrule.byDay = [{ day: WeekDayCodes[start.getDay()], offset: pos }];
                if (counterEvent.rrule.frequency === "YEARLY") {
                    counterEvent.rrule.byMonth = start.getMonth();
                }
            }
        }
    }
}

function matchCounter(counter, originator, recuridIsoDate) {
    const sameOriginator = counter.originator.email === originator;
    const noRecurid = !recuridIsoDate && !counter.counter.recurid;
    const sameRecurid = recuridIsoDate && counter.counter.recurid && recuridIsoDate === counter.counter.recurid.iso8601;
    return sameOriginator && (noRecurid || sameRecurid);
}

/** Build a "standard" occurrence of the event based on the recurrence identifier. */
function buildOccurrence(event, recurid) {
    const occurrence = JSON.parse(JSON.stringify(event.value.main));
    occurrence.rrule = null;
    occurrence.recurid = recurid;
    occurrence.dtstart = recurid;
    const main = event.value.main;
    const duration = new Date(main.dtend.iso8601).getTime() - new Date(main.dtstart.iso8601).getTime();
    occurrence.dtend.iso8601 = new Date(new Date(recurid.iso8601).getTime() + duration).toISOString();
    return occurrence;
}

/** Reset participation status of each attendee. */
function resetParticipationStatuses(event, recuridIsoDate) {
    const attendees = recuridIsoDate
        ? event.value.occurrences.find(o => o.recurid.iso8601 === recuridIsoDate).attendees
        : event.value.main.attendees;
    attendees.forEach(a => {
        a.rsvp = true;
        a.partStatus = "NeedsAction";
    });
}

/** Recurrent event may have an ending limit, 'until'. Make sure this limit is coherent. */
function adjustUntil(event) {
    if (event.rrule && event.rrule.until) {
        const untilDate = new Date(event.rrule.until.iso8601);
        const startDate = new Date(event.dtstart.iso8601);
        untilDate.setHours(startDate.getHours());
        untilDate.setMinutes(startDate.getMinutes());
        untilDate.setSeconds(startDate.getSeconds());
        untilDate.setMilliseconds(startDate.getMilliseconds());
        event.rrule.until.iso8601 = untilDate.toISOString();
    }
}
