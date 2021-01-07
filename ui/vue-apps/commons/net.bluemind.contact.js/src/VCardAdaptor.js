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
    }
};
