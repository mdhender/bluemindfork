import { VCardKind } from "@bluemind/addressbook.api";
import UUIDGenerator from "@bluemind/uuid";

import VCardAdaptor from "./VCardAdaptor";
import VCardInfoAdaptor from "./VCardInfoAdaptor";

function createFromRecipient({ dn, address }, kind = VCardKind.individual) {
    return {
        uid: UUIDGenerator.generate(),
        address,
        dn,
        kind,
        photo: false
    };
}

export { createFromRecipient, VCardAdaptor, VCardInfoAdaptor };
