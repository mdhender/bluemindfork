import ContactValidator from "./ContactValidator";
import DirEntryAdaptor from "./DirEntryAdaptor";
import RecipientAdaptor from "./RecipientAdaptor";
import VCardAdaptor from "./VCardAdaptor";
import VCardInfoAdaptor from "./VCardInfoAdaptor";
import { VCard, VCardQuery } from "@bluemind/addressbook.api";
import { EmailExtractor } from "@bluemind/email";
import { inject } from "@bluemind/inject";

const SEARCH_API_MAX_SIZE = 10000;

const Fields = {
    NAME: "value.identification.formatedName.value",
    EMAIL: "value.communications.emails.value",
    COMPANY: "value.organizational.org.company"
};

function buildFieldsQuery(fields, value) {
    return fields.reduce((query, field) => {
        const esEquals = `${field}:(${value})`;
        return query ? `${query} OR ${esEquals}` : esEquals;
    }, "");
}

function searchVCardsHelper(
    pattern,
    {
        size = 5,
        noGroup = false,
        addressBook = null,
        fields = [Fields.NAME, Fields.EMAIL],
        from = 0,
        orderBy = VCardQuery.OrderBy.Pertinance
    } = {}
) {
    if (size < 0) {
        size = SEARCH_API_MAX_SIZE;
    }

    const escaped = Array.isArray(pattern)
        ? pattern.reduce((escaped, current, index) => escaped + (index !== 0 ? " OR " : "") + escape(current), "")
        : escape(pattern);

    const groupPart = noGroup ? "" : "(value.kind:group AND _exists_:value.organizational.member) OR ";
    const fieldsPart = fields.length ? `(${buildFieldsQuery(fields, escaped)}) AND ` : "";
    const containerPart = addressBook ? ` AND containerUid:${escape(addressBook)}` : "";
    const esQuery = `${fieldsPart}(${groupPart}_exists_:value.communications.emails.value)${containerPart}`;

    return { from, size, query: esQuery, orderBy, escapeQuery: false };
}

function searchVCardsByIdHelper(uid, containerUid, size = 5, noGroup = false) {
    const groupPart = noGroup ? "" : "(value.kind:group AND _exists_:value.organizational.member) OR ";
    const containerPart = containerUid ? ` AND containerUid:${escape(containerUid)}` : "";
    const esQuery =
        "(uid:" + escape(uid) + containerPart + ") AND (" + groupPart + "_exists_:value.communications.emails.value)";
    return { from: 0, size, query: esQuery, orderBy: VCardQuery.OrderBy.Pertinance, escapeQuery: false };
}

function recipientStringToVCardItem(recipientString) {
    const address = EmailExtractor.extractEmail(recipientString);
    const displayName = EmailExtractor.extractDN(recipientString) || guessName(address);
    return {
        value: {
            identification: { formatedName: { parameters: [], value: displayName } },
            communications: { emails: [{ parameters: [], value: address }] }
        }
    };
}
function guessName(address) {
    return address
        ?.split("@")[0]
        ?.split(".")
        .map(token => token.charAt(0).toUpperCase() + token.slice(1).toLowerCase())
        .join(" ");
}

function sortAddressBooks(addressBooks, userId) {
    return addressBooks.sort((a, b) => {
        if (!isShared(a, userId) != !isShared(b, userId)) return !isShared(a, userId) ? -1 : 1;

        if (isDirectoryAddressBook(a.uid, a.domainUid) != isDirectoryAddressBook(b.uid, b.domainUid))
            return isDirectoryAddressBook(a.uid, a.domainUid) ? -1 : 1;

        if (isPersonalAddressBook(a.uid, a.owner) != isPersonalAddressBook(b.uid, b.owner))
            return isPersonalAddressBook(a.uid, a.owner) ? -1 : 1;

        if (isCollectAddressBook(a.uid, a.owner) != isCollectAddressBook(b.uid, b.owner))
            return isCollectAddressBook(a.uid, a.owner) ? -1 : 1;

        return a.name.localeCompare(b.name);
    });
}
function isShared(addressBook, userId) {
    return !isDirectoryAddressBook(addressBook.uid, addressBook.domainUid) && userId !== addressBook.owner;
}
function isDirectoryAddressBook(contactContainerUid, domain) {
    return contactContainerUid === `addressbook_${domain}`;
}

function isPersonalAddressBook(contactContainerUid, userId) {
    return contactContainerUid === `book:Contacts_${userId}`;
}

function isCollectAddressBook(contactContainerUid, userId) {
    return contactContainerUid === `book:CollectedContacts_${userId}`;
}

/** Recursively fetch members having an address. */
async function fetchMembersWithAddress(contactContainerUid, contactUid) {
    const vCard = await fetchContact(contactContainerUid, contactUid);
    const members = vCard?.value.organizational?.member;
    return members?.length
        ? (
            await Promise.all(
                members.map(async m =>
                    m.mailto
                        ? {
                            address: m.mailto,
                            dn: m.dn || m.commonName,
                            containerUid: m.containerUid,
                            uid: m.itemUid,
                            memberCount: m.memberCount || 0,
                            kind: m.kind || VCard.Kind.individual
                        }
                        : await fetchMembersWithAddress(m.containerUid || contactContainerUid, m.itemUid)
                )
            )
        ).flatMap(r => r)
        : [];
}

/** Fetch first level members, with extended info. */
async function fetchContactMembers(containerUid, contactUid) {
    const vCard = await fetchContact(containerUid, contactUid);
    const members = vCard?.value.organizational?.member;
    if (!members?.length) {
        return [];
    }
    const vCardLikeList = await fetchContacts(
        members.map(m => ({ containerUid: m.containerUid || containerUid, uid: m.itemUid }))
    );

    const fetchedVCards = vCardLikeList.map(vcardLike => ({
        ...VCardAdaptor.toContact(vcardLike),
        uid: vcardLike.uid,
        urn: `${vcardLike.uid}@${vcardLike.containerUid}`
    }));

    const missingVCards = members
        .filter(
            m =>
                !vCardLikeList.some(
                    vCardLike =>
                        vCardLike.containerUid === (m.containerUid || containerUid) && vCardLike.uid === m.itemUid
                )
        )
        .map(m => ({ kind: VCard.Kind.individual, dn: m.commonName, address: m.mailto }));

    return [...fetchedVCards, ...missingVCards];
}

function fetchContact(containerUid, uid) {
    if (uid) {
        try {
            return inject("AddressBookPersistence", containerUid).getComplete(uid);
        } catch {
            return null;
        }
    }
}

async function fetchContacts(ids) {
    const groupedByContainer = ids.reduce((res, { containerUid, uid }) => {
        if (!res[containerUid]) {
            res[containerUid] = [];
        }
        if (uid) {
            res[containerUid].push(uid);
        }
        return res;
    }, {});

    try {
        const promises = Object.keys(groupedByContainer).map(async containerUid =>
            (await inject("AddressBookPersistence", containerUid).multipleGet(groupedByContainer[containerUid]))
                .map(res => (res.uid ? { ...res, containerUid } : undefined))
                .filter(Boolean)
        );
        return (await Promise.all(promises)).flatMap(r => r);
    } catch {
        return [];
    }
}

function hasMailOrMember(vCard) {
    return (
        (vCard.value.kind === "group" && vCard.value.organizational?.member) ||
        vCard.value.communications?.emails?.length
    );
}

function contactContainerUid(contact) {
    return contact.urn?.split("@")[1];
}

function removeDuplicatedContacts(contacts) {
    return contacts.reduce(
        (allContacts, current) => {
            if (!containsContact(allContacts, current)) {
                allContacts.push(current);
            }
            return allContacts;
        },
        []
    );
}

function containsContact(contacts, contact) {
    return contacts.some(c =>
        contact.address
            ? c.address === contact.address
            : contact.kind === "group"
                ? contact.dn === c.dn && contact.members?.length === c.members?.length
                : false
    );
}

export {
    contactContainerUid,
    ContactValidator,
    DirEntryAdaptor,
    fetchContactMembers,
    fetchMembersWithAddress,
    Fields,
    guessName,
    hasMailOrMember,
    isCollectAddressBook,
    isDirectoryAddressBook,
    isPersonalAddressBook,
    RecipientAdaptor,
    recipientStringToVCardItem,
    removeDuplicatedContacts,
    searchVCardsByIdHelper,
    searchVCardsHelper,
    sortAddressBooks,
    VCardAdaptor,
    VCardInfoAdaptor
};

function escape(term) {
    const charsToEscape = ["\\", "+", "-", "&&", "||", "!", "(", ")", "{", "}", "[", "]", "^", '"', "~", "*", "?", ":"];
    for (let i = 0; i < charsToEscape.length; i++) {
        term = term.split(charsToEscape[i]).join("\\" + charsToEscape[i]);
    }
    return term;
}
