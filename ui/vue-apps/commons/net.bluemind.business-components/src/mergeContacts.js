import get from "lodash.get";
import isEqual from "lodash.isequal";
import { isCollectAddressBook, isDomainAddressBook, isPersonalAddressBook } from "@bluemind/contact";
import { inject } from "@bluemind/inject";

export default async function merge(contactsWithContainerUid) {
    const sortedContacts = await sortContacts(contactsWithContainerUid);
    const bestContactForIdentification = bestContact(sortedContacts, "value.identification.formatedName.value");
    const photoBinary = await photoUrl(bestContact(sortedContacts, "value.identification.photo"));
    return {
        value: {
            identification: {
                ...bestContactForIdentification.value?.identification,
                photo: !!photoBinary,
                photoBinary
            },
            communications: {
                tels: mergeList(sortedContacts, "value.communications.tels"),
                emails: mergeList(sortedContacts, "value.communications.emails", "value")
            },
            deliveryAddressing: mergeList(sortedContacts, "value.deliveryAddressing")
        },
        uid: bestContactForIdentification.uid,
        containerUid: bestContactForIdentification.containerUid
    };
}

function bestContact(sortedContacts, propPath) {
    return sortedContacts.find(c => !!get(c, propPath, c));
}

function mergeList(sortedContacts, listPath, propPath) {
    const items = [];
    sortedContacts.forEach(c =>
        get(c, listPath)?.forEach(item => {
            const itemValue = get(item, propPath, item);
            if (!items.some(i => isEqual(get(i, propPath, i), itemValue))) {
                items.push(item);
            }
        })
    );
    return items;
}

async function sortContacts(contacts) {
    const { userId, domain } = inject("UserSession");
    return [...contacts].sort((a, b) => {
        return priority(b.containerUid, userId, domain) - priority(a.containerUid, userId, domain);
    });
}

function priority(containerUid, userId, domain) {
    if (isDomainAddressBook(containerUid, domain)) {
        return 3;
    }
    if (isPersonalAddressBook(containerUid, userId)) {
        return 2;
    }
    if (isCollectAddressBook(containerUid, userId)) {
        return 0;
    }
    return 1;
}

async function photoUrl(contact) {
    if (contact) {
        return `${window.location.protocol}//${window.location.hostname}/api/addressbooks/${contact.containerUid}/${contact.uid}/photo`;
    }
}
