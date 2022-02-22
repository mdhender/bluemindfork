export function create(uid, address, dn, kind, photo = false, containerUid = "", isInternal = false) {
    const urn = uid && containerUid ? uid + "@" + containerUid : "";
    return { uid, address, dn, kind, photo, urn, isInternal };
}
