import { VCard } from "@bluemind/addressbook.api";
import UUIDGenerator from "@bluemind/uuid";
import { create } from "./model";

export default {
    toContact({ dn, address, kind = VCard.Kind.individual, memberCount, uid, containerUid }) {
        return create(uid || UUIDGenerator.generate(), address, dn, kind, false, containerUid, false, memberCount);
    },
    toContacts(recipients) {
        return recipients.map(this.toContact);
    }
};
