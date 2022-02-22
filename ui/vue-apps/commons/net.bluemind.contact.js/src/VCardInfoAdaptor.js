import { create } from "./model";

export default {
    toContact(vCardInfo) {
        const uid = vCardInfo.uid;
        return create(
            uid,
            vCardInfo.value.mail,
            vCardInfo.value.formatedName,
            vCardInfo.value.kind,
            vCardInfo.value.photo,
            vCardInfo.containerUid,
            !!vCardInfo.value.source
        );
    },
    toVCardInfo(contact) {
        return {
            containerUid: contact.urn ? contact.urn.split("@")[1] : "",
            value: {
                kind: contact.kind,
                mail: contact.address,
                formatedName: contact.dn,
                tel: "",
                categories: [],
                photo: contact.photo,
                source: ""
            }
        };
    }
};
