import { MimeType } from "@bluemind/email";
import injector from "@bluemind/inject";
import ItemUri from "@bluemind/item-uri";

export function selectMessage({ dispatch, commit, state, getters }, messageKey) {
    const userSession = injector.getProvider("UserSession").get();
    const CAPABILITIES = userSession.roles.includes("hasCalendar")
        ? [MimeType.TEXT_HTML, MimeType.TEXT_PLAIN, MimeType.TEXT_CALENDAR]
        : [MimeType.TEXT_HTML, MimeType.TEXT_PLAIN];

    if (state.currentFolderKey && !ItemUri.isItemUri(messageKey)) {
        messageKey = ItemUri.encode(parseInt(messageKey), getters.currentFolder.uid);
    }
    if (state.currentMessage.key !== messageKey) {
        return dispatch("$_getIfNotPresent", [messageKey]).then(messages => {
            commit("currentMessage/update", { key: messageKey });
            const parts = messages[0].computeParts();
            const inlines = parts.inlines.find(part =>
                part.capabilities.every(capability => CAPABILITIES.includes(capability))
            );
            commit("currentMessage/setParts", { attachments: parts.attachments, inlines: inlines.parts });
            let promises = inlines.parts.map(part =>
                dispatch("messages/fetch", { messageKey, part, isAttachment: false })
            );
            return Promise.all(promises);
        });
    }
    return Promise.resolve();
}
