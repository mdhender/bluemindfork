import { inject } from "@bluemind/inject";
import { ContainerType } from "../../container";
import {
    defaultAddressBookDomainAcl,
    defaultAddressBookDirEntryAcl,
    verbToAddressBookAcl,
    getAddressBookOptions,
    addressBookAclToVerb,
    AddressBookAcl
} from "./AddressBookShareHelper";
import {
    defaultCalendarDomainAcl,
    defaultCalendarDirEntryAcl,
    mergeFreebusyWithCalAcl,
    verbToCalendarAcl,
    getCalendarOptions,
    calendarAclToVerb,
    CalendarAcl
} from "./CalendarShareHelper";
import {
    defaultMailboxDirEntryAcl,
    getMailboxOptions,
    MailboxAcl,
    mailboxAclToVerb,
    verbToMailboxAcl
} from "./MailboxShareHelper";
import {
    defaultTodoListDirEntryAcl,
    defaultTodoListDomainAcl,
    getTodoListOptions,
    TodoListAcl,
    todoListAclToVerb,
    verbToTodoListAcl
} from "./TodoListShareHelper";

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
            const acl = verbToAcl(verb, container.type);
            return { ...entry, acl };
        });
    }

    if (isMyDefaultCalendar) {
        ({ domainAcl, dirEntriesAcl } = await handleFreebusy(domainAcl, dirEntriesAcl, container));
    }

    return { domainAcl, dirEntriesAcl };
}

export function defaultDirEntryAcl(containerType) {
    switch (containerType) {
        case ContainerType.ADDRESSBOOK:
            return defaultAddressBookDirEntryAcl();
        case ContainerType.CALENDAR:
            return defaultCalendarDirEntryAcl();
        case ContainerType.MAILBOX:
            return defaultMailboxDirEntryAcl();
        case ContainerType.TODOLIST:
            return defaultTodoListDirEntryAcl();
    }
}

export function getOptions(containerType, count, i18n, isMyDefaultCalendar) {
    switch (containerType) {
        case ContainerType.ADDRESSBOOK:
            return getAddressBookOptions(i18n, count);
        case ContainerType.CALENDAR:
            return getCalendarOptions(i18n, count, isMyDefaultCalendar);
        case ContainerType.MAILBOX:
            return getMailboxOptions(i18n);
        case ContainerType.TODOLIST:
            return getTodoListOptions(i18n, count);
    }
}

export function aclToVerb(containerType, acl, isFreebusy = false) {
    switch (containerType) {
        case ContainerType.ADDRESSBOOK:
            return addressBookAclToVerb(acl);
        case ContainerType.CALENDAR:
            return calendarAclToVerb(acl, isFreebusy);
        case ContainerType.MAILBOX:
            return mailboxAclToVerb(acl);
        case ContainerType.TODOLIST:
            return todoListAclToVerb(acl);
    }
}

export function noRightAcl(containerType) {
    switch (containerType) {
        case ContainerType.ADDRESSBOOK:
            return AddressBookAcl.HAS_NO_RIGHTS;
        case ContainerType.CALENDAR:
            return CalendarAcl.CANT_INVITE_ME;
        case ContainerType.MAILBOX:
            return MailboxAcl.HAS_NO_RIGHTS;
        case ContainerType.TODOLIST:
            return TodoListAcl.HAS_NO_RIGHTS;
    }
}

function loadDomainAcl(aclList, containerType) {
    const hasDomainAcl = aclList.find(acl => acl.subject === inject("UserSession").domain);
    return hasDomainAcl ? verbToAcl(hasDomainAcl.verb, containerType) : defaultDomainAcl(containerType);
}

function verbToAcl(verb, containerType) {
    switch (containerType) {
        case ContainerType.ADDRESSBOOK:
            return verbToAddressBookAcl(verb);
        case ContainerType.CALENDAR:
            return verbToCalendarAcl(verb);
        case ContainerType.MAILBOX:
            return verbToMailboxAcl(verb);
        case ContainerType.TODOLIST:
            return verbToTodoListAcl(verb);
    }
}

function defaultDomainAcl(containerType) {
    switch (containerType) {
        case ContainerType.ADDRESSBOOK:
            return defaultAddressBookDomainAcl();
        case ContainerType.CALENDAR:
            return defaultCalendarDomainAcl();
        case ContainerType.TODOLIST:
            return defaultTodoListDomainAcl();
    }
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

async function handleFreebusy(domainAcl, dirEntriesAcl, { owner }) {
    const aclList = await inject("ContainerManagementPersistence", "freebusy:" + owner).getAccessControlList();
    return await mergeFreebusyWithCalAcl(aclList, domainAcl, dirEntriesAcl);
}
