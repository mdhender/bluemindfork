import { Verb } from "@bluemind/core.container.api";
import { inject } from "@bluemind/inject";
import { MimeType } from "@bluemind/email";

export const ContainerType = {
    ADDRESSBOOK: "addressbook",
    CALENDAR: "calendar",
    MAILBOX: "mailboxacl",
    TODOLIST: "todolist"
};

export function containerToSubscription({ uid, name, offlineSync }) {
    const userSession = inject("UserSession");
    return {
        value: {
            containerUid: uid,
            offlineSync: offlineSync,
            containerType: "calendar",
            owner: userSession.userId,
            defaultContainer: false,
            name: name
        },
        uid: "sub-of-" + userSession.userId + "-to-" + uid,
        internalId: null,
        version: 0,
        displayName: uid,
        externalId: null,
        createdBy: userSession.userId,
        updatedBy: userSession.userId,
        created: Date.now(),
        updated: Date.now(),
        flags: []
    };
}

export function containerToCalendarDescriptor({ name, settings }) {
    const userSession = inject("UserSession");
    return {
        domainUid: userSession.domain,
        name: name,
        owner: userSession.userId,
        settings
    };
}

export function containerToAddressBookDescriptor({ name, settings }) {
    const userSession = inject("UserSession");
    return {
        domainUid: userSession.domain,
        name: name,
        owner: userSession.userId,
        settings,
        system: false
    };
}

export function containerToModifiableDescriptor({ defaultContainer, name }) {
    return { name, defaultContainer, deleted: false };
}

export function matchingIcon(containerType) {
    switch (containerType) {
        case ContainerType.CALENDAR:
            return "calendar";
        case ContainerType.MAILBOX:
            return "user-enveloppe";
        case ContainerType.ADDRESSBOOK:
            return "addressbook";
        case ContainerType.TODOLIST:
            return "list";
    }
}

export function matchingFileTypeIcon(containerType) {
    switch (containerType) {
        case ContainerType.TODOLIST:
        case ContainerType.CALENDAR:
            return "file-type-ics";
        case ContainerType.ADDRESSBOOK:
            return "file-type-vcard";
        default:
            return "file-type-unknown";
    }
}

export function allowedFileTypes(containerType) {
    switch (containerType) {
        case ContainerType.TODOLIST:
        case ContainerType.CALENDAR:
            return MimeType.TEXT_CALENDAR || MimeType.ICS || MimeType.TEXT_PLAIN;
        case ContainerType.ADDRESSBOOK:
            return MimeType.VCARD;
    }
}

export async function importFileRequest(containerType, containerUid, file, uploadCanceller) {
    // be careful here: we expect request to return a task ref
    if (containerType === ContainerType.CALENDAR) {
        return inject("VEventPersistence", containerUid).importIcs(file, uploadCanceller);
    } else if (containerType === ContainerType.ADDRESSBOOK) {
        const encoded = await file.text().then(res => JSON.stringify(res));
        return inject("VCardServicePersistence", containerUid).importCards(encoded, uploadCanceller);
    } else if (containerType === ContainerType.TODOLIST) {
        const encoded = await file.text().then(res => JSON.stringify(res));
        return inject("VTodoPersistence", containerUid).importIcs(encoded, uploadCanceller);
    }
}

export function isDefault(containerUid) {
    const prefixes = ["calendar:Default:", "book:Contacts_", "book:CollectedContacts_", "todolist:default_"];
    return prefixes.some(prefix => containerUid.startsWith(prefix));
}

export function isManaged(container) {
    return container.verbs.some(verb => verb === Verb.All || verb === Verb.Manage);
}

export function create(type) {
    const userSession = inject("UserSession");
    const container = {
        uid: "",
        name: "",
        owner: userSession.userId,
        offlineSync: true,
        type,
        defaultContainer: false,
        readOnly: false,
        domainUid: userSession.domain,
        ownerDisplayname: userSession.formatedName,
        ownerDirEntryPath: userSession.domain + "/users/" + userSession.userId,
        settings: {},
        deleted: false
    };
    if (type === ContainerType.CALENDAR) {
        container.settings = { ...container.settings, type: "internal" };
    }
    return container;
}
