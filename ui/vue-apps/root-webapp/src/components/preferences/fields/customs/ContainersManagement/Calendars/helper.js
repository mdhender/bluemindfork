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

const HANDLED_VERBS = [Verb.All, Verb.Manage, Verb.Write, Verb.Read, Verb.Invitation];

export default {
    matchingIcon: () => "calendar",
    matchingFileTypeIcon: () => "file-type-ics",
    allowedFileTypes: () => MimeType.TEXT_CALENDAR || MimeType.ICS || MimeType.TEXT_PLAIN,
    importFileRequest: (containerUid, file, uploadCanceller) =>
        inject("VEventPersistence", containerUid).importIcs(file, uploadCanceller),
    buildDefaultDirEntryAcl: dirEntry => [{ subject: dirEntry.uid, verb: Verb.Invitation }],
    defaultDomainAcl: [],
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
    aclToOption(acl, isFreebusy = false) {
        const verbs = acl.map(({ verb }) => verb);

        if (verbs.includes(Verb.All) || (verbs.includes(Verb.Write) && verbs.includes(Verb.Manage))) {
            return CalendarAcl.CAN_MANAGE_SHARES;
        }
        if (verbs.includes(Verb.Write)) {
            return CalendarAcl.CAN_EDIT_MY_EVENTS;
        }
        if (verbs.includes(Verb.Read) && isFreebusy) {
            return CalendarAcl.CAN_SEE_MY_AVAILABILITY;
        }
        if (verbs.includes(Verb.Read)) {
            return CalendarAcl.CAN_SEE_MY_EVENTS;
        }
        if (verbs.includes(Verb.Invitation)) {
            return CalendarAcl.CAN_INVITE_ME;
        }
        return CalendarAcl.CANT_INVITE_ME;
    },
    updateAcl(acl, subject, option, isFreebusy) {
        if (this.aclToOption(acl) !== option) {
            const newAcl = acl.flatMap(ac => (!HANDLED_VERBS.includes(ac.verb) ? ac : []));
            if (isFreebusy) {
                switch (option) {
                    case CalendarAcl.CANT_INVITE_ME:
                    case CalendarAcl.CAN_INVITE_ME:
                        throw "impossible case : no acl equivalent";
                    case CalendarAcl.CAN_SEE_MY_AVAILABILITY:
                    case CalendarAcl.CAN_SEE_MY_EVENTS:
                    case CalendarAcl.CAN_EDIT_MY_EVENTS:
                        newAcl.push({ verb: Verb.Read, subject });
                        break;
                    case CalendarAcl.CAN_MANAGE_SHARES:
                        newAcl.push({ verb: Verb.Write, subject });
                        newAcl.push({ verb: Verb.Manage, subject });
                        break;
                }
            } else {
                switch (option) {
                    case CalendarAcl.CANT_INVITE_ME:
                        throw "impossible case : no acl equivalent";
                    case CalendarAcl.CAN_INVITE_ME:
                    case CalendarAcl.CAN_SEE_MY_AVAILABILITY:
                        newAcl.push({ verb: Verb.Invitation, subject });
                        break;
                    case CalendarAcl.CAN_SEE_MY_EVENTS:
                        newAcl.push({ verb: Verb.Read, subject });
                        break;
                    case CalendarAcl.CAN_EDIT_MY_EVENTS:
                        newAcl.push({ verb: Verb.Write, subject });
                        break;
                    case CalendarAcl.CAN_MANAGE_SHARES:
                        newAcl.push({ verb: Verb.Write, subject });
                        newAcl.push({ verb: Verb.Manage, subject });
                        break;
                }
            }
            return newAcl;
        }
    }
};
