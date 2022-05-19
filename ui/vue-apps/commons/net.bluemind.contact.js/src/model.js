export function create(uid, address, dn, kind, photo = false, containerUid = "", isInternal = false, memberCount = 0) {
    const urn = uid && containerUid ? uid + "@" + containerUid : "";
    const entries = kind === "group" ? Array(memberCount).fill({}) : [{ address, dn }];
    return { uid, entries, dn, kind, photo, urn, isInternal };
}
