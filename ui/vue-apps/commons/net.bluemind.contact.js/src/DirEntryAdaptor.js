import { inject } from "@bluemind/inject";
import { create } from "./model";

export default {
    toContact(dirEntry) {
        const containerUid = "addressbook_" + inject("UserSession").domain;
        const uid = dirEntry.value.entryUid;
        return create(
            uid,
            dirEntry.value.email,
            dirEntry.value.displayName,
            dirEntry.value.kind,
            false,
            containerUid,
            true
        );
    },
    toDirEntry(contact) {
        return {
            uid: contact.uid,
            value: {
                entryUid: contact.uid,
                email: contact.address,
                displayName: contact.dn,
                kind: contact.kind
            }
        };
    }
};
