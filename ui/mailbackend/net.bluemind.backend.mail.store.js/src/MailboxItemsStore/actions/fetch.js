import ServiceLocator from "@bluemind/inject";
import { MimeType } from "@bluemind/email";
import { ItemUri } from "@bluemind/item-uri";

export function fetch({ state, commit }, { messageKey, part, isAttachment }) {
    const container = ItemUri.container(messageKey);
    const item = state.items[messageKey];
    let encoding = part.encoding;
    if (isAttachment || MimeType.isImage(part)) {
        encoding = null;
    }
    return ServiceLocator.getProvider("MailboxItemsPersistence")
        .get(container)
        .fetch(item.value.imapUid, part.address, { encoding, mime: part.mime, charset: part.charset })
        .then(stream => commit("storePartContent", { messageKey, address: part.address, content: stream }));
}
