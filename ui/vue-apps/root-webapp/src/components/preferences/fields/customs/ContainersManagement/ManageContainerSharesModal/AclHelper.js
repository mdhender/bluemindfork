import { inject } from "@bluemind/inject";
import { ContainerType } from "../container";
import {
    defaultCalendarDomainAcl,
    defaultCalendarDirEntryAcl,
    mergeFreebusyWithCalAcl,
    verbToCalendarAcl
} from "./CalendarAclHelper";
import { defaultMailboxDirEntryAcl, verbToMailboxAcl } from "./MailboxAclHelper";

export async function loadAcl(container, isMyDefaultCalendar) {
    let domainAcl = null,
        dirEntriesAcl = [];
    const userSession = inject("UserSession");

    let aclList = await inject("ContainerManagementPersistence", container.uid).getAccessControlList();
    if (container.type === ContainerType.CALENDAR) {
        const hasDomainAcl = aclList.find(acl => acl.subject === userSession.domain);
        if (hasDomainAcl) {
            domainAcl = verbToCalendarAcl(hasDomainAcl.verb);
        } else {
            domainAcl = defaultCalendarDomainAcl();
        }
    }

    aclList = aclList.filter(
        acl =>
            acl.subject !== userSession.domain &&
            acl.subject !== userSession.userId &&
            acl.subject !== container.owner &&
            !acl.subject.startsWith("x-calendar-")
    );

    if (aclList.length > 0) {
        const dirEntries = await inject("DirectoryPersistence").getMultiple(aclList.map(acl => acl.subject));
        dirEntriesAcl = dirEntries.map(entry => {
            const verb = aclList.find(acl => acl.subject === entry.uid).verb;
            let acl;
            if (container.type === ContainerType.CALENDAR) {
                acl = verbToCalendarAcl(verb);
            } else if (container.type === ContainerType.MAILBOX) {
                acl = verbToMailboxAcl(verb);
            }
            return { ...entry, acl };
        });
    }

    if (isMyDefaultCalendar) {
        // load freebusy acl
        const aclList = await inject(
            "ContainerManagementPersistence",
            "freebusy:" + container.owner
        ).getAccessControlList();

        const mergeResult = await mergeFreebusyWithCalAcl(aclList, domainAcl, dirEntriesAcl);
        domainAcl = mergeResult.domainAcl;
        dirEntriesAcl = mergeResult.dirEntriesAcl;
    }

    return { domainAcl, dirEntriesAcl };
}

export function defaultDirEntryAcl(containerType) {
    if (containerType === ContainerType.CALENDAR) {
        return defaultCalendarDirEntryAcl();
    } else if (containerType === ContainerType.MAILBOX) {
        return defaultMailboxDirEntryAcl();
    }
}
