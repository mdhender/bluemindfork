import { VCard } from "@bluemind/addressbook.api";
import { ItemValue } from "@bluemind/core.container.api";
import { create } from "./model";

export default {
    toVCard(contact: { kind: VCard.Kind; dn: string; address: string; photo?: boolean; pem: string }): VCard {
        return {
            kind: contact.kind,
            identification: {
                formatedName: { value: contact.dn },
                photo: contact.photo
            },
            communications: {
                emails: [{ value: contact.address }]
            },
            security: {
                key: {
                    value: contact.pem
                }
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
