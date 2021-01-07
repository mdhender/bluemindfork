import { Kind } from "@bluemind/addressbook.api";
import UUIDGenerator from "@bluemind/uuid";

import AddressbookAdaptor from "./AddressbookAdaptor";
import VCardAdaptor from "./VCardAdaptor";
import VCardInfoAdaptor from "./VCardInfoAdaptor";

function createContact({ dn, address }, kind = Kind.individual) {
    return {
        uid: UUIDGenerator.generate(),
        address,
        dn,
        kind,
        photo: false
    };
}

export { AddressbookAdaptor, createContact, VCardAdaptor, VCardInfoAdaptor };
