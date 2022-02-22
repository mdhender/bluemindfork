import DirEntryAdaptor from "./DirEntryAdaptor";
import RecipientAdaptor from "./RecipientAdaptor";
import VCardAdaptor from "./VCardAdaptor";
import VCardInfoAdaptor from "./VCardInfoAdaptor";

function getQuery(pattern) {
    return (
        "(value.identification.formatedName.value:" +
        pattern +
        " OR value.communications.emails.value:" +
        pattern +
        ") AND _exists_:value.communications.emails.value"
    );
}

export { DirEntryAdaptor, getQuery, RecipientAdaptor, VCardAdaptor, VCardInfoAdaptor };
