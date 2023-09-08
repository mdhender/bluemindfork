import { Verb } from "@bluemind/core.container.api";
import { inject } from "@bluemind/inject";
import { ContainerHelper, ContainerType } from "../container";

export async function loadAcl(container, isMyDefaultCalendar) {
    let domainAcl = [],
        dirEntriesAcl = [];

    let aclList = await inject("ContainerManagementPersistence", container.uid).getAccessControlList();
    if (container.type !== ContainerType.MAILBOX) {
        domainAcl = loadDomainAcl(aclList, container.type);
    }

    aclList = aclList.filter(acl => filterDirEntryAcl(acl, container));

    const subjects = aclList.reduce(
        (subjects, { subject }) => (subjects.includes(subject) ? subjects : [...subjects, subject]),
        []
    );

    if (aclList.length > 0) {
        const dirEntries = await inject("DirectoryPersistence").getMultiple(subjects);
        dirEntriesAcl = dirEntries.map(entry => {
            const acl = aclList.filter(acl => acl.subject === entry.uid);
            return { ...entry, acl };
        });
    }

    if (isMyDefaultCalendar) {
        ({ domainAcl, dirEntriesAcl } = await handleFreebusy(domainAcl, dirEntriesAcl, container));
    }

    return { domainAcl, dirEntriesAcl };
}

function loadDomainAcl(aclList, containerType) {
    const helper = ContainerHelper.use(containerType);
    const domainAcl = aclList.filter(acl => acl.subject === inject("UserSession").domain);
    return domainAcl.length ? domainAcl : helper.defaultDomainAcl;
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
async function handleFreebusy(domainAcl, dirEntriesAcl, { owner }) {
    let aclList = await inject("ContainerManagementPersistence", "freebusy:" + owner).getAccessControlList();
    const userSession = inject("UserSession");
    const hasDomainAcl = aclList.find(acl => acl.subject === userSession.domain);
    if (hasDomainAcl) {
        domainAcl = adaptAclForFreebusy(domainAcl, hasDomainAcl.verb, userSession.domain);
    }

    aclList = aclList.filter(acl => acl.subject !== userSession.domain && acl.subject !== userSession.userId);
    const dirEntriesWithOnlyFreebusyAcl = [];
    aclList.forEach(acl => {
        const dirEntry = dirEntriesAcl.find(entry => acl.subject === entry.uid);
        if (dirEntry) {
            dirEntry.acl = adaptAclForFreebusy(dirEntry.acl, acl.verb, dirEntry.uid);
        } else {
            dirEntriesWithOnlyFreebusyAcl.push(acl);
        }
    });
    if (dirEntriesWithOnlyFreebusyAcl.length > 0) {
        let dirEntries = await inject("DirectoryPersistence").getMultiple(
            dirEntriesWithOnlyFreebusyAcl.map(acl => acl.subject)
        );
        dirEntries = dirEntries.map(entry => {
            const acl = aclList.filter(acl => acl.subject === entry.uid);
            return { ...entry, acl };
        });
        dirEntriesAcl.push(...dirEntries);
    }

    return { domainAcl, dirEntriesAcl };
}

function adaptAclForFreebusy(calendarAcl, freebusyVerb, subject) {
    if (
        freebusyVerb === Verb.Read &&
        !calendarAcl.some(({ verb }) => [Verb.Invitation, Verb.Read, Verb.Write, Verb.Manage, Verb.All].includes(verb))
    ) {
        return [{ subject, verb: Verb.Invitation }];
    }
    if (freebusyVerb === Verb.All) {
        return [
            { subject, verb: Verb.Write },
            { subject, verb: Verb.Manage }
        ];
    }
    return calendarAcl;
}
