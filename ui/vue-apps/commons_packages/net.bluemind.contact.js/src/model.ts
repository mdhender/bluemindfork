import { VCard } from "@bluemind/addressbook.api";
import { Contact } from "@bluemind/contact";

export function create(
    uid?: string,
    address?: string,
    dn?: string,
    kind?: VCard.Kind,
    photo = false,
    containerUid = "",
    isInternal = false,
    memberCount = 0
): Contact {
    const urn = uid && containerUid ? uid + "@" + containerUid : "";
    const members = Array(memberCount >= 0 ? memberCount : 0).fill({});
    return { uid, address, dn, kind, photo, urn, isInternal, members };
}
