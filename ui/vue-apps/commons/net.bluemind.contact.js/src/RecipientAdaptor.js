import { VCardKind } from "@bluemind/addressbook.api";
import UUIDGenerator from "@bluemind/uuid";
import { create } from "./model";

export default {
    toContact({ dn, address }, kind = VCardKind.individual) {
        return create(UUIDGenerator.generate(), address, dn, kind);
    },
    toContacts(recipients) {
        return recipients.map(this.toContact);
    }
};
