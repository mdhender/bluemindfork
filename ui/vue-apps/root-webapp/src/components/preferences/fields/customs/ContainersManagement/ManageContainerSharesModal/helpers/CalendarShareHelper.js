import { Verb } from "@bluemind/core.container.api";
import { inject } from "@bluemind/inject";

/**
 * BlueMind core API have 2 distincts acl data linked to default user calendar:
 *      - freebusy container
 *      - calendar container
 * This helper allows to unify those ACL by ordering them.
 * Used in share modal UI where this complexity is hidden by proposing only one select-box to manage calendar rights.
 */
export const CalendarAcl = {
    CANT_INVITE_ME: 0,
    CAN_INVITE_ME: 1,
    CAN_SEE_MY_AVAILABILITY: 2,
    CAN_SEE_MY_EVENTS: 3,
    CAN_EDIT_MY_EVENTS: 4,
    CAN_MANAGE_SHARES: 5
};

export function calendarAclToVerb(acl, isFreebusy) {
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
}

export async function mergeFreebusyWithCalAcl(aclList, domainAcl, dirEntriesAcl) {
    const userSession = inject("UserSession");
    const hasDomainAcl = aclList.find(acl => acl.subject === userSession.domain);
    if (hasDomainAcl) {
        domainAcl = adaptAclForFreebusy(domainAcl, hasDomainAcl.verb);
    }

    aclList = aclList.filter(acl => acl.subject !== userSession.domain && acl.subject !== userSession.userId);
    const dirEntriesWithOnlyFreebusyAcl = [];
    aclList.forEach(acl => {
        const dirEntry = dirEntriesAcl.find(entry => acl.subject === entry.uid);
        if (dirEntry) {
            dirEntry.acl = adaptAclForFreebusy(dirEntry.acl, acl.verb);
        } else {
            dirEntriesWithOnlyFreebusyAcl.push(acl);
        }
    });
    if (dirEntriesWithOnlyFreebusyAcl.length > 0) {
        let dirEntries = await inject("DirectoryPersistence").getMultiple(
            dirEntriesWithOnlyFreebusyAcl.map(acl => acl.subject)
        );
        dirEntries = dirEntries.map(entry => {
            const acl = verbToCalendarAcl(aclList.find(acl => acl.subject === entry.uid).verb, true);
            return { ...entry, acl };
        });
        dirEntriesAcl.push(...dirEntries);
    }

    return { domainAcl, dirEntriesAcl };
}

export function verbToCalendarAcl(verb, isFreebusy = false) {
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

export function defaultCalendarDomainAcl() {
    return CalendarAcl.CANT_INVITE_ME;
}

export function defaultCalendarDirEntryAcl() {
    return CalendarAcl.CAN_INVITE_ME;
}

function adaptAclForFreebusy(calendarAcl, freebusyVerb) {
    if (freebusyVerb === Verb.Read && calendarAcl < CalendarAcl.CAN_SEE_MY_AVAILABILITY) {
        return CalendarAcl.CAN_SEE_MY_AVAILABILITY;
    }
    if (freebusyVerb === Verb.All) {
        return CalendarAcl.CAN_MANAGE_SHARES;
    }
    return calendarAcl;
}

export function getCalendarOptions(i18n, count, isMyDefaultCalendar) {
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
}
