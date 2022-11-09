import get from "lodash.get";
import isEqual from "lodash.isequal";
import { inject } from "@bluemind/inject";

export default async function merge(contacts, contactContainerUids) {
    if (contacts?.length !== contactContainerUids?.length) {
        throw "'contacts' and 'contactContainerUids' must have the same length";
    }

    const contactsWithContainerUid = contacts.map((c, index) => ({ ...c, containerUid: contactContainerUids[index] }));
    const sortedContacts = await sortContacts(contactsWithContainerUid);
    const bestContactForIdentification = bestContact(sortedContacts, "value.identification.formatedName.value");
    const photoBinary = await fetchPhoto(bestContact(sortedContacts, "value.identification.photo"));
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
    if (containerUid === `addressbook_${domain}`) {
        return 3;
    }
    if (containerUid === `book:Contacts_${userId}`) {
        return 2;
    }
    if (containerUid === `book:CollectedContacts_${userId}`) {
        return 0;
    }
    return 1;
}

async function fetchPhoto(contact) {
    if (contact) {
        const photoURL = `api/addressbooks/${contact.containerUid}/${contact.uid}/photo`;
        return await fetch(photoURL);
    }
}
