import { MimeType } from "@bluemind/email";

const CAPABILITIES = [MimeType.TEXT_HTML, MimeType.TEXT_PLAIN];

export function selectMessage({ dispatch, commit, state }, messageKey) {
    if (state.currentMessageKey != messageKey) {
        return dispatch("$_getIfNotPresent", messageKey).then(message => {
            commit("setCurrentMessage", messageKey);
            const parts = message.computeParts();
            const inlines = parts.inlines.find(part =>
                part.capabilities.every(capability => CAPABILITIES.includes(capability))
            );
            commit("setCurrentMessageParts", { attachments: parts.attachments, inlines: inlines.parts });
            let promises = inlines.parts.map(part =>
                dispatch("messages/fetch", { messageKey, part, isAttachment: false })
            );
            return Promise.all(promises);
        });
    }
    return Promise.resolve();
}
