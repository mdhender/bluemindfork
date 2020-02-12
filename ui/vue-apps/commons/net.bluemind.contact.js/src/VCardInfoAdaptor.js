export default {
    toContact(vCardInfo) {
        return {
            email: vCardInfo.value.mail,
            formattedName: vCardInfo.displayName,
            kind: vCardInfo.value.kind,
            photo: vCardInfo.value.photo,
            uid: vCardInfo.uid
        };
    }
};
