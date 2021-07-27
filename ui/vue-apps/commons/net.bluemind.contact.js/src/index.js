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

function getQuery(pattern) {
    return (
        "(value.identification.formatedName.value:" +
        pattern +
        " OR value.communications.emails.value:" +
        pattern +
        ") AND _exists_:value.communications.emails.value"
    );
}

export { createFromRecipient, getQuery, VCardAdaptor, VCardInfoAdaptor };
