import { MimeType } from "@bluemind/email";
import injector from "@bluemind/inject";
import ItemUri from "@bluemind/item-uri";

export function selectMessage({ dispatch, commit, state, getters }, messageKey) {
    const userSession = injector.getProvider("UserSession").get();
    const CAPABILITIES = [MimeType.TEXT_HTML, MimeType.TEXT_PLAIN];

    if (state.currentFolderKey && !ItemUri.isItemUri(messageKey)) {
        messageKey = ItemUri.encode(parseInt(messageKey), getters.currentFolder.uid);
    }

    if (state.currentMessage.key !== messageKey) {
        return dispatch("$_getIfNotPresent", [messageKey]).then(messages => {
            const message = messages[0];
            const promises = [];
            commit("currentMessage/update", { key: messageKey });

            if (userSession.roles.includes("hasCalendar") && !message.ics.isEmpty) {
                promises.push(dispatch("fetchEvent", message.ics.eventUid));
            }

            const parts = message.computeParts();
            const inlines = parts.inlines.find(part =>
                part.capabilities.every(capability => CAPABILITIES.includes(capability))
            );
            commit("currentMessage/setParts", { attachments: parts.attachments, inlines: inlines.parts });
            promises.push(
                ...inlines.parts.map(part => dispatch("messages/fetch", { messageKey, part, isAttachment: false }))
            );
            return Promise.all(promises);
        });
    }
    return Promise.resolve();
}
