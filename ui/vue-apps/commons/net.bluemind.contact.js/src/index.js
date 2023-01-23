import ContactValidator from "./ContactValidator";
import DirEntryAdaptor from "./DirEntryAdaptor";
import RecipientAdaptor from "./RecipientAdaptor";
import VCardAdaptor from "./VCardAdaptor";
import VCardInfoAdaptor from "./VCardInfoAdaptor";
import { VCardKind, VCardQueryOrderBy } from "@bluemind/addressbook.api";
import { EmailExtractor } from "@bluemind/email";
import { inject } from "@bluemind/inject";

const SEARCH_API_MAX_SIZE = 10000;

function searchVCardsHelper(pattern, size = 5, noGroup = false) {
    if (size < 0) {
        size = SEARCH_API_MAX_SIZE;
    }
    const escaped = escape(pattern);
    const groupPart = noGroup ? "" : "(value.kind:group AND _exists_:value.organizational.member) OR ";
    const esQuery =
        "(value.identification.formatedName.value:" +
        escaped +
        " OR value.communications.emails.value:" +
        escaped +
        ") AND (" +
        groupPart +
        "_exists_:value.communications.emails.value)";
    return { from: 0, size, query: esQuery, orderBy: VCardQueryOrderBy.Pertinance, escapeQuery: false };
}

function searchVCardsByIdHelper(uid, containerUid, size = 5, noGroup = false) {
    const groupPart = noGroup ? "" : "(value.kind:group AND _exists_:value.organizational.member) OR ";
    const containerPart = containerUid ? ` AND containerUid:${escape(containerUid)}` : "";
    const esQuery =
        "(uid:" + escape(uid) + containerPart + ") AND (" + groupPart + "_exists_:value.communications.emails.value)";
    return { from: 0, size, query: esQuery, orderBy: VCardQueryOrderBy.Pertinance, escapeQuery: false };
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
                                kind: m.kind || VCardKind.individual
                            }
                          : await fetchMembersWithAddress(m.containerUid, m.itemUid)
                  )
              )
          ).flatMap(r => r)
        : [];
}

/** Fetch first level members, with extended info. */
async function fetchContactMembers(containerUid, contactUid) {
    const vCard = await fetchContact(containerUid, contactUid);
    const members = vCard?.value.organizational?.member;
    const res = members?.length
        ? (await fetchContacts(members.map(m => ({ containerUid: m.containerUid, uid: m.itemUid })))).map(
              vcardLike => ({
                  ...VCardAdaptor.toContact(vcardLike),
                  uid: vcardLike.uid,
                  urn: `${vcardLike.uid}@${vcardLike.containerUid}`
              })
          )
        : [];
    return res;
}

function fetchContact(containerUid, uid) {
    return inject("AddressBookPersistence", containerUid).getComplete(uid);
}

async function fetchContacts(ids) {
    const groupedByContainer = ids.reduce((res, { containerUid, uid }) => {
        if (!res[containerUid]) {
            res[containerUid] = [];
        }
        res[containerUid].push(uid);
        return res;
    }, {});

    const promises = Object.keys(groupedByContainer).map(async containerUid =>
        (
            await inject("AddressBookPersistence", containerUid).multipleGet(groupedByContainer[containerUid])
        ).map(res => ({ ...res, containerUid }))
    );
    return (await Promise.all(promises)).flatMap(r => r);
}

export {
    ContactValidator,
    DirEntryAdaptor,
    fetchContactMembers,
    fetchMembersWithAddress,
    isCollectAddressBook,
    isDirectoryAddressBook,
    isPersonalAddressBook,
    RecipientAdaptor,
    recipientStringToVCardItem,
    searchVCardsByIdHelper,
    searchVCardsHelper,
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
