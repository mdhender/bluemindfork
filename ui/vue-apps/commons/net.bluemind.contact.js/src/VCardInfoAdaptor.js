export default {
    toContact(vCardInfo) {
        const uid = vCardInfo.uid;
        return {
            address: vCardInfo.value.mail,
            dn: vCardInfo.value.formatedName,
            kind: vCardInfo.value.kind,
            photo: vCardInfo.value.photo,
            uid,
            urn: uid && vCardInfo.containerUid ? uid + "@" + vCardInfo.containerUid : ""
        };
    }
};
