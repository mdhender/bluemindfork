import DirEntryAdaptor from "./DirEntryAdaptor";
import RecipientAdaptor from "./RecipientAdaptor";
import VCardAdaptor from "./VCardAdaptor";
import VCardInfoAdaptor from "./VCardInfoAdaptor";
import { VCardQueryOrderBy } from "@bluemind/addressbook.api";

function searchVCardsHelper(pattern, size = 5, noGroup = false) {
    const escaped = escape(pattern);
    const groupPart = noGroup ? "" : "value.kind:group OR ";
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

export { DirEntryAdaptor, searchVCardsHelper, RecipientAdaptor, VCardAdaptor, VCardInfoAdaptor };

function escape(term) {
    const charsToEscape = ["\\", "+", "-", "&&", "||", "!", "(", ")", "{", "}", "[", "]", "^", '"', "~", "*", "?", ":"];
    for (let i = 0; i < charsToEscape.length; i++) {
        term = term.split(charsToEscape[i]).join("\\" + charsToEscape[i]);
    }
    return term;
}
