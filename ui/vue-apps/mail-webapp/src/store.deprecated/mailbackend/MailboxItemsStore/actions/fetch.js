import { inject } from "@bluemind/inject";
import { MimeType } from "@bluemind/email";
import { ItemUri } from "@bluemind/item-uri";

export async function fetch({ getters, commit }, { messageKey, part, isAttachment }) {
    const container = ItemUri.container(messageKey);
    const item = getters["getMessageByKey"](messageKey);

    const stream = await inject("MailboxItemsPersistence", container).fetch(
        item.imapUid,
        part.address,
        part.encoding,
        part.mime,
        part.charset
    );
    if (!isAttachment && (MimeType.isText(part) || MimeType.isHtml(part) || MimeType.isCalendar(part))) {
        return new Promise(resolve => {
            const reader = new FileReader();
            reader.readAsText(stream, part.encoding);
            reader.addEventListener("loadend", e => {
                commit("storePartContent", { messageKey, address: part.address, content: e.target.result });
                resolve();
            });
        });
    } else {
        commit("storePartContent", { messageKey, address: part.address, content: stream });
    }
}
