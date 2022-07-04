import { create } from "./model";

export default {
    toContact(vCardInfo) {
        return create(
            vCardInfo.uid,
            vCardInfo.value.mail,
            vCardInfo.value.formatedName,
            vCardInfo.value.kind,
            vCardInfo.value.photo,
            vCardInfo.containerUid,
            !!vCardInfo.value.source,
            vCardInfo.value.memberCount
        );
    },
    toVCardInfo(contact) {
        return {
            containerUid: contact.urn ? contact.urn.split("@")[1] : "",
            uid: contact.uid,
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
