import ContactValidator from "./ContactValidator";
import DirEntryAdaptor from "./DirEntryAdaptor";
import RecipientAdaptor from "./RecipientAdaptor";
import VCardAdaptor from "./VCardAdaptor";
import VCardInfoAdaptor from "./VCardInfoAdaptor";
import { VCardQueryOrderBy } from "@bluemind/addressbook.api";
import { EmailExtractor } from "@bluemind/email";

function searchVCardsHelper(pattern, size = 5, noGroup = false) {
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

export {
    ContactValidator,
    DirEntryAdaptor,
    searchVCardsByIdHelper,
    searchVCardsHelper,
    RecipientAdaptor,
    recipientStringToVCardItem,
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
