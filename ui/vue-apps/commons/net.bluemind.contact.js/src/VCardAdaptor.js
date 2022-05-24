import { create } from "./model";

export default {
    toVCard(contact) {
        return {
            kind: contact.kind,
            identification: {
                formatedName: { value: contact.dn },
                photo: contact.photo
            },
            communications: {
                emails: [{ value: contact.address }]
            }
        };
    },
    toContact(vCard) {
        return create(
            vCard.uid,
            vCard.value.communications?.emails[0]?.value,
            vCard.value.identification?.formatedName?.value,
            vCard.value.kind,
            undefined,
            undefined,
            undefined,
            vCard.value.organizational?.member?.length
        );
    }
};
