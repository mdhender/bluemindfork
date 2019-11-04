import ServiceLocator from "@bluemind/inject";
import { MimeType } from "@bluemind/email";

export function fetch({ state, commit }, { folder, id, part, isAttachment }) {
    const item = state.items[id];
    let encoding = part.encoding;
    if (isAttachment || MimeType.isImage(part)) {
        encoding = null;
    }
    return ServiceLocator.getProvider("MailboxItemsPersistence")
        .get(folder)
        .fetch(item.value.imapUid, part.address, encoding, part.mime, part.charset)
        .then(stream => {
            if (isAttachment && part.encoding == "8bit") {
                stream = btoa(stream);
            }
            commit("storePart", { id, address: part.address, content: stream }); 
        });
}
