import { MimeType } from "@bluemind/email";

const CAPABILITIES = [MimeType.TEXT_HTML, MimeType.TEXT_PLAIN];

export function selectMessage({ dispatch, commit, state }, messageId) {
    if (state.currentMessageId != messageId) {
        return dispatch("$_getIfNotPresent", { folder: state.currentFolderUid, id: messageId }).then(message => {
            commit("setCurrentMessage", messageId);
            const parts = message.computeParts();
            const inlines = parts.inlines.find(part =>
                part.capabilities.every(capability => CAPABILITIES.includes(capability))
            );
            commit("setCurrentMessageParts", { attachments: parts.attachments, inlines: inlines.parts });
            let promises = inlines.parts.map(part =>
                dispatch("messages/fetch", { folder: state.currentFolderUid, id: messageId, part, isAttachment: false })
            );
            return Promise.all(promises);
        });
    }
}
