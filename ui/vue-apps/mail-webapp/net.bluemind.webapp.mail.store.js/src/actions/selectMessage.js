import { MimeType } from "@bluemind/email";

const CAPABILITIES = [MimeType.TEXT_HTML, MimeType.TEXT_PLAIN];

export function selectMessage({ dispatch, commit, state }, messageKey) {
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
