import UUIDGenerator from "@bluemind/uuid";
import { AddressBookClient, VCard } from "@bluemind/addressbook.api";
import { VCardAdaptor, searchVCardsHelper } from "@bluemind/contact";
import session from "../service-worker/environnment/session";

export default async function (pem: string, dn: string, email: string): Promise<void> {
    const sid = await session.sid;
    const userId = await session.userId;
    const personalAddressBookUid = `book:Contacts_${userId}`;
    const contact = {
        uid: UUIDGenerator.generate(),
        kind: VCard.Kind.individual,
        address: email,
        dn,
        pem
    };
    const client = new AddressBookClient(sid, personalAddressBookUid);
    const vCard = VCardAdaptor.toVCard(contact);
    const existingContact = await client.search(searchVCardsHelper(email));
    if (existingContact.values!.length === 0) {
        return client.create(contact.uid, vCard);
    }
    return client.update(existingContact.values![0]!.uid!, vCard);
}
