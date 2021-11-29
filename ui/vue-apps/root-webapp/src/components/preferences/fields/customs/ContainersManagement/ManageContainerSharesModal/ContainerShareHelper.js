import { Verb } from "@bluemind/core.container.api";
import { inject } from "@bluemind/inject";
import { ContainerHelper, ContainerType } from "../container";
import { CalendarAcl } from "../Calendars/helper";

export async function loadAcl(container, isMyDefaultCalendar) {
    let domainAcl = -1,
        dirEntriesAcl = [];

    let aclList = await inject("ContainerManagementPersistence", container.uid).getAccessControlList();
    if (container.type !== ContainerType.MAILBOX) {
        domainAcl = loadDomainAcl(aclList, container.type);
    }

    aclList = aclList.filter(acl => filterDirEntryAcl(acl, container));

    if (aclList.length > 0) {
        const dirEntries = await inject("DirectoryPersistence").getMultiple(aclList.map(acl => acl.subject));
        dirEntriesAcl = dirEntries.map(entry => {
            const verb = aclList.find(acl => acl.subject === entry.uid).verb;
            const acl = ContainerHelper.use(container.type).verbToAcl(verb);
            return { ...entry, acl };
        });
    }

    if (isMyDefaultCalendar) {
        ({ domainAcl, dirEntriesAcl } = await handleFreebusy(domainAcl, dirEntriesAcl, container));
    }

    return { domainAcl, dirEntriesAcl };
}

function loadDomainAcl(aclList, containerType) {
    const hasDomainAcl = aclList.find(acl => acl.subject === inject("UserSession").domain);
    const helper = ContainerHelper.use(containerType);
    return hasDomainAcl ? helper.verbToAcl(hasDomainAcl.verb) : helper.defaultDomainAcl;
}

function filterDirEntryAcl(acl, { owner }) {
    const userSession = inject("UserSession");
    return (
        acl.subject !== userSession.domain &&
        acl.subject !== userSession.userId &&
        acl.subject !== owner &&
        !acl.subject.startsWith("x-calendar-")
    );
}

/**
 * BlueMind core API have 2 distincts acl data linked to default user calendar:
 *      - freebusy container
 *      - calendar container
 * This helper allows to unify those ACL by ordering them.
 * Used in share modal UI where this complexity is hidden by proposing only one select-box to manage calendar rights.
 */
async function handleFreebusy(domainAcl, dirEntriesAcl, { owner, type }) {
    let aclList = await inject("ContainerManagementPersistence", "freebusy:" + owner).getAccessControlList();
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
            const acl = ContainerHelper.use(type).verbToAcl(aclList.find(acl => acl.subject === entry.uid).verb, true);
            return { ...entry, acl };
        });
        dirEntriesAcl.push(...dirEntries);
    }

    return { domainAcl, dirEntriesAcl };
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
