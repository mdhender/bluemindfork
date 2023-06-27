import { VCard } from "@bluemind/addressbook.api";
import UUIDGenerator from "@bluemind/uuid";
import { create } from "./model";

export default {
    toContact({ dn, address, kind = VCard.Kind.individual, memberCount, uid, containerUid }) {
        return create(uid || UUIDGenerator.generate(), address, dn, kind, false, containerUid, false, memberCount);
    },
    toContacts(recipients) {
        return recipients?.map(this.toContact);
    },
    fromContact({ uid, address, dn, kind, photo, urn, isInternal, members }) {
        return {
            uid,
            address,
            dn,
            kind,
            photo,
            containerUid: urn?.split("@")[1],
            isInternal,
            memberCount: members?.length || 0
        };
    },
    fromContacts(contacts) {
        return contacts?.map(this.fromContact);
    }
};
