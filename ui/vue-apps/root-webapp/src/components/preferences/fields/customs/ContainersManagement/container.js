import { Verb } from "@bluemind/core.container.api";
import { inject } from "@bluemind/inject";

export const ContainerType = {
    ADDRESSBOOK: "addressbook",
    CALENDAR: "calendar",
    MAILBOX: "mailboxacl",
    TODOLIST: "todolist"
};

const helpers = new Map();
export const ContainerHelper = {
    register: (type, helper) => {
        helpers.set(type, helper);
    },
    use: type => {
        if (helpers.has(type)) {
            return helpers.get(type);
        }
        throw new Error("No helper registered for type ", type);
    }
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

export function isDefault(containerUid) {
    const prefixes = ["calendar:Default:", "book:Contacts_", "book:CollectedContacts_", "todolist:default_"];
    return prefixes.some(prefix => containerUid.startsWith(prefix));
}

export function isManaged(container) {
    return container.verbs.some(verb => verb === Verb.All || verb === Verb.Manage);
}

export function adapt(container) {
    return { ...container, defaultContainer: isDefault(container.uid), name: container.name.trim() };
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
