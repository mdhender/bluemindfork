export default {
    toContact(vCardInfo) {
        return {
            address: vCardInfo.value.mail,
            dn: vCardInfo.value.formatedName,
            kind: vCardInfo.value.kind,
            photo: vCardInfo.value.photo,
            uid: vCardInfo.uid
        };
    }
};
