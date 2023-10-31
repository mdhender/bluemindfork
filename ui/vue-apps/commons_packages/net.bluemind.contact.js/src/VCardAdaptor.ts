import { VCard } from "@bluemind/addressbook.api";
import { ItemValue } from "@bluemind/core.container.api";
import { create } from "./model";

export default {
    toVCard(contact: { kind: VCard.Kind; dn: string; address: string; photo?: boolean; pem: string }): VCard {
        const [givenNames, familyNames] = contact.dn.split(" ");
        return {
            kind: contact.kind,
            identification: {
                name: { givenNames: givenNames || "", familyNames: familyNames || "", value: "" },
                photo: contact.photo
            },
            communications: {
                emails: [{ value: contact.address }]
            },
            security: {
                keys: [{ value: contact.pem }]
            }
        };
    },
    toContact(vCard: ItemValue<VCard>) {
        const email: string = vCard.value.communications?.emails ? vCard.value.communications?.emails[0]?.value : "";
        return create(
            vCard.uid,
            email,
            vCard.value.identification?.formatedName?.value,
            vCard.value.kind,
            undefined,
            undefined,
            undefined,
            vCard.value.organizational?.member?.length
        );
    }
};
