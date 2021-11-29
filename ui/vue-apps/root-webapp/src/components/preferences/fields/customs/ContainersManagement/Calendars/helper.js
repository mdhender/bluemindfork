import { Verb } from "@bluemind/core.container.api";
import { MimeType } from "@bluemind/email";
import { inject } from "@bluemind/inject";

export const CalendarAcl = {
    CANT_INVITE_ME: 0,
    CAN_INVITE_ME: 1,
    CAN_SEE_MY_AVAILABILITY: 2,
    CAN_SEE_MY_EVENTS: 3,
    CAN_EDIT_MY_EVENTS: 4,
    CAN_MANAGE_SHARES: 5
};

export default {
    matchingIcon: () => "calendar",
    matchingFileTypeIcon: () => "file-type-ics",
    allowedFileTypes: () => MimeType.TEXT_CALENDAR || MimeType.ICS || MimeType.TEXT_PLAIN,
    importFileRequest: (containerUid, file, uploadCanceller) =>
        inject("VEventPersistence", containerUid).importIcs(file, uploadCanceller),
    defaultDirEntryAcl: CalendarAcl.CAN_INVITE_ME,
    defaultDomainAcl: CalendarAcl.CANT_INVITE_ME,
    noRightAcl: CalendarAcl.CANT_INVITE_ME,
    getOptions: (i18n, count, isMyDefaultCalendar) => {
        const options = [
            {
                text: i18n.tc("preferences.calendar.my_calendars.cant_invite_me_to_a_meeting", count),
                value: CalendarAcl.CANT_INVITE_ME
            },
            {
                text: i18n.tc("preferences.calendar.my_calendars.can_invite_me_to_a_meeting", count),
                value: CalendarAcl.CAN_INVITE_ME
            },
            {
                text: i18n.tc("preferences.calendar.my_calendars.can_invite_me_to_a_meeting_and_see_my_events", count),
                value: CalendarAcl.CAN_SEE_MY_EVENTS
            },
            {
                text: i18n.tc("preferences.calendar.my_calendars.can_edit_my_events", count),
                value: CalendarAcl.CAN_EDIT_MY_EVENTS
            },
            {
                text: i18n.tc("preferences.calendar.my_calendars.can_edit_my_events_and_manage_shares", count),
                value: CalendarAcl.CAN_MANAGE_SHARES
            }
        ];
        if (isMyDefaultCalendar) {
            options.splice(2, 0, {
                text: i18n.tc(
                    "preferences.calendar.my_calendars.can_invite_me_to_a_meeting_and_see_my_availability",
                    count
                ),
                value: CalendarAcl.CAN_SEE_MY_AVAILABILITY
            });
        }
        return options;
    },
    aclToVerb: (acl, isFreebusy) => {
        if (isFreebusy) {
            switch (acl) {
                case CalendarAcl.CANT_INVITE_ME:
                case CalendarAcl.CAN_INVITE_ME:
                    throw "impossible case : no acl equivalent";
                case CalendarAcl.CAN_SEE_MY_AVAILABILITY:
                case CalendarAcl.CAN_SEE_MY_EVENTS:
                case CalendarAcl.CAN_EDIT_MY_EVENTS:
                    return Verb.Read;
                case CalendarAcl.CAN_MANAGE_SHARES:
                    return Verb.All;
            }
        }
        switch (acl) {
            case CalendarAcl.CANT_INVITE_ME:
                throw "impossible case : no acl equivalent";
            case CalendarAcl.CAN_INVITE_ME:
            case CalendarAcl.CAN_SEE_MY_AVAILABILITY:
                return Verb.Invitation;
            case CalendarAcl.CAN_SEE_MY_EVENTS:
                return Verb.Read;
            case CalendarAcl.CAN_EDIT_MY_EVENTS:
                return Verb.Write;
            case CalendarAcl.CAN_MANAGE_SHARES:
                return Verb.All;
        }
    },
    verbToAcl: (verb, isFreebusy = false) => {
        if (verb === Verb.All) {
            return CalendarAcl.CAN_MANAGE_SHARES;
        }
        if (isFreebusy && verb === Verb.Read) {
            return CalendarAcl.CAN_SEE_MY_AVAILABILITY;
        }
        switch (verb) {
            case Verb.Invitation:
                return CalendarAcl.CAN_INVITE_ME;
            case Verb.Read:
                return CalendarAcl.CAN_SEE_MY_EVENTS;
            case Verb.Write:
                return CalendarAcl.CAN_EDIT_MY_EVENTS;
        }
    }
};
