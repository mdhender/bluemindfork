import { Verb } from "@bluemind/core.container.api";
import { MimeType } from "@bluemind/email";
import { inject } from "@bluemind/inject";
import { loadCalendarUrls } from "../ManageContainerSharesModal/ExternalShareHelper";
import { isDefault } from "../container";

export const CalendarRight = {
    CANT_INVITE_ME: 1,
    CAN_INVITE_ME: 2,
    CAN_SEE_MY_AVAILABILITY: 3,
    CAN_SEE_MY_EVENTS: 4,
    CAN_EDIT_MY_EVENTS: 5,
    CAN_MANAGE_SHARES: 6
};

const HANDLED_VERBS = [Verb.All, Verb.Manage, Verb.Write, Verb.Read, Verb.Invitation];

// do not lose Access Controls with other verbs than HANDLED_VERBS
let otherAcl = [];
let otherFreebusyAcl = [];

export default {
    matchingIcon: () => "calendar",
    matchingFileTypeIcon: () => "file-type-ics",
    allowedFileTypes: () => MimeType.TEXT_CALENDAR || MimeType.ICS || MimeType.TEXT_PLAIN,
    importFileRequest: (containerUid, file, uploadCanceller) =>
        inject("VEventPersistence", containerUid).importIcs(file, uploadCanceller),
    defaultUserRight: CalendarRight.CAN_EDIT_MY_EVENTS,
    defaultDomainRight: CalendarRight.CAN_INVITE_ME,
    maxRight: CalendarRight.CAN_MANAGE_SHARES,
    readRight: CalendarRight.CAN_SEE_MY_EVENTS,
    getOptions: (i18n, container) => {
        const options = [
            {
                text: i18n.t("preferences.calendar.my_calendars.cant_invite_me_to_a_meeting"),
                value: CalendarRight.CANT_INVITE_ME
            },
            {
                text: i18n.t("preferences.calendar.my_calendars.can_invite_me_to_a_meeting"),
                value: CalendarRight.CAN_INVITE_ME
            },
            {
                text: i18n.t("preferences.calendar.my_calendars.can_invite_me_to_a_meeting_and_see_my_events"),
                value: CalendarRight.CAN_SEE_MY_EVENTS
            },
            {
                text: i18n.t("preferences.calendar.my_calendars.can_edit_my_events"),
                value: CalendarRight.CAN_EDIT_MY_EVENTS
            },
            {
                text: i18n.t("preferences.calendar.my_calendars.can_edit_my_events_and_manage_shares"),
                value: CalendarRight.CAN_MANAGE_SHARES
            }
        ];
        if (container.owner === inject("UserSession").userId && isDefault(container.uid)) {
            options.splice(2, 0, {
                text: i18n.t("preferences.calendar.my_calendars.can_invite_me_to_a_meeting_and_see_my_availability"),
                value: CalendarRight.CAN_SEE_MY_AVAILABILITY
            });
        }
        return options;
    },
    async loadRights(container) {
        const allAcl = await inject("ContainerManagementPersistence", container.uid).getAccessControlList();
        const aclReducer = (res, ac) => {
            HANDLED_VERBS.includes(ac.verb) ? res[0].push(ac) : res[1].push(ac);
            return res;
        };
        const [acl, other] = allAcl.reduce(aclReducer, [[], []]);
        otherAcl = other;

        const allFreebusyAcl = await inject(
            "ContainerManagementPersistence",
            "freebusy:" + container.owner
        ).getAccessControlList();
        const [freebusyAcl, otherFreebusy] = allFreebusyAcl.reduce(aclReducer, [[], []]);
        otherFreebusyAcl = otherFreebusy;

        const { domain: domainUid, userId } = inject("UserSession");
        const domain = aclToRight(domainUid, acl, freebusyAcl, this.defaultDomainRight);

        const userUids = new Set(
            [...acl, ...freebusyAcl].flatMap(({ subject }) =>
                subject !== domainUid &&
                subject !== userId &&
                subject !== container.owner &&
                !subject.startsWith("x-calendar-")
                    ? subject
                    : []
            )
        );
        const users = {};
        userUids.forEach(userUid => {
            users[userUid] = aclToRight(userUid, acl, freebusyAcl, this.defaultUserRight);
        });

        const external = await loadCalendarUrls(container.uid);

        return { users, domain, external };
    },
    saveRights(rightBySubject, container) {
        return Promise.all(
            rightsToAcls(rightBySubject, container).map(({ containerUid, acl }) => {
                inject("ContainerManagementPersistence", containerUid).setAccessControlList(acl);
            })
        );
    }
};

function aclToRight(subjectUid, acl, freebusyAcl, defaultRight) {
    const extractVerbs = acl => acl.flatMap(({ subject, verb }) => (subject === subjectUid ? verb : []));
    const verbs = extractVerbs(acl);
    const freebusyVerbs = extractVerbs(freebusyAcl);
    return verbsToRight(verbs, freebusyVerbs, defaultRight);
}

function verbsToRight(verbs, freebusyVerbs, defaultRight) {
    if (verbs.includes(Verb.All) || (verbs.includes(Verb.Write) && verbs.includes(Verb.Manage))) {
        return CalendarRight.CAN_MANAGE_SHARES;
    }
    if (verbs.includes(Verb.Write)) {
        return CalendarRight.CAN_EDIT_MY_EVENTS;
    }
    if (
        freebusyVerbs.includes(Verb.Read) &&
        !verbs.some(v => [Verb.Invitation, Verb.Read, Verb.Write, Verb.Manage, Verb.All].includes(v))
    ) {
        return CalendarRight.CAN_SEE_MY_AVAILABILITY;
    }
    if (verbs.includes(Verb.Read)) {
        return CalendarRight.CAN_SEE_MY_EVENTS;
    }
    if (verbs.includes(Verb.Invitation)) {
        return CalendarRight.CAN_INVITE_ME;
    }
    return defaultRight;
}

function rightsToAcls(rightBySubject, container) {
    const acl = [];
    const freebusyAcl = [];

    Object.entries(rightBySubject).forEach(([subject, right]) => {
        switch (right) {
            case CalendarRight.CAN_INVITE_ME:
                acl.push({ subject, verb: Verb.Invitation });
                break;
            case CalendarRight.CAN_SEE_MY_AVAILABILITY:
                acl.push({ subject, verb: Verb.Invitation });
                freebusyAcl.push({ subject, verb: Verb.Read });
                break;
            case CalendarRight.CAN_SEE_MY_EVENTS:
                acl.push({ subject, verb: Verb.Read });
                freebusyAcl.push({ subject, verb: Verb.Read });
                break;
            case CalendarRight.CAN_EDIT_MY_EVENTS:
                acl.push({ subject, verb: Verb.Write });
                freebusyAcl.push({ subject, verb: Verb.Read });
                break;
            case CalendarRight.CAN_MANAGE_SHARES:
                acl.push({ subject, verb: Verb.Write });
                acl.push({ subject, verb: Verb.Manage });
                freebusyAcl.push({ subject, verb: Verb.Write });
                freebusyAcl.push({ subject, verb: Verb.Manage });
                break;
            case CalendarRight.CANT_INVITE_ME:
            default:
                break;
        }
    });

    return [
        { containerUid: container.uid, acl: acl.concat(otherAcl) },
        { containerUid: "freebusy:" + container.owner, acl: freebusyAcl.concat(otherFreebusyAcl) }
    ];
}
