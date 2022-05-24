export function create(uid, address, dn, kind, photo = false, containerUid = "", isInternal = false, memberCount = 0) {
    const urn = uid && containerUid ? uid + "@" + containerUid : "";
    const members = Array(memberCount >= 0 ? memberCount : 0).fill({});
    return { uid, address, dn, kind, photo, urn, isInternal, members };
}
