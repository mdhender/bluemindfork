import ServiceLocator from "@bluemind/inject";
import { MimeType } from "@bluemind/email";
import { ItemUri } from "@bluemind/item-uri";

export function fetch({ state, commit }, { messageKey, part, isAttachment }) {
    const container = ItemUri.container(messageKey);
    const item = state.items[messageKey];

    return ServiceLocator.getProvider("MailboxItemsPersistence")
        .get(container)
        .fetch(item.value.imapUid, part.address, part.encoding, part.mime, part.charset)
        .then(stream => {
            if (!isAttachment && (MimeType.isText(part) || MimeType.isHtml(part))) {
                const reader = new FileReader();
                reader.readAsText(stream, part.encoding);
                reader.addEventListener('loadend', (e) => {
                    commit("storePartContent", { messageKey, address: part.address, content: e.target.result });
                });
            } else {
                commit("storePartContent", { messageKey, address: part.address, content: stream });
            }
        });
}
