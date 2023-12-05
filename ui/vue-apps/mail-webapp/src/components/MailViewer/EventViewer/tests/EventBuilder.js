import EventHelper from "../../../../store/helpers/EventHelper";
import { AttendeeFactory } from "./AttendeeFactory";

export function EventBuilder(event = eventDefault(), args = defaultFuncArgs()) {
    return {
        removeCalendarOwnerFromAttendeesList() {
            event.attendees = event.attendees.filter(anAttendee => !anAttendee.equals(args.calendarOwner));
            return EventBuilder(event, args);
        },
        isWritable(val) {
            args.setWritableState(val);
            return EventBuilder(event, args);
        },
        build() {
            return EventHelper.adapt(_eventJson(event), ...args.toList());
        }
    };

    function _eventJson(event) {
        return {
            value: {
                main: {
                    ...event,
                    attendees: event.attendees.map(A => A.toJson())
                }
            }
        };
    }
}

function defaultFuncArgs() {
    const state = {
        mailBoxOwner: "C162C72B-5130-42AD-A545-9600AB95E0ED",
        originator: "room31@devenv.dev.bluemind.net",
        recuridIsoDate: null,
        calendarUid: "calendar:C162C72B-5130-42AD-A545-9600AB95E0ED",
        calendarOwner: "C162C72B-5130-42AD-A545-9600AB95E0ED",
        isWritable: true
    };
    return {
        get calendarOwner() {
            return state.calendarOwner;
        },

        setWritableState(value) {
            state.isWritable = value;
        },

        toList() {
            return [
                state.mailBoxOwner,
                state.originator,
                state.recuridIsoDate,
                state.calendarUid,
                state.calendarOwner,
                state.isWritable
            ];
        }
    };
}

function eventDefault() {
    return {
        dtstart: {
            iso8601: "2023-12-01T16:30:00.000+01:00",
            timezone: "Europe/Paris",
            precision: "DateTime"
        },
        dtend: {
            iso8601: "2023-12-01T17:30:00.000+01:00",
            timezone: "Europe/Paris",
            precision: "DateTime"
        },
        organizer: {
            uri: null,
            commonName: "ROOM31",
            mailto: "room31@devenv.dev.bluemind.net",
            dir: "bm://75a0d5b3.internal/resources/C162C72B-5130-42AD-A545-9600AB95E0ED"
        },
        attendees: [AttendeeFactory.OWNER(), AttendeeFactory.INDIVIDUAL()]
    };
}
