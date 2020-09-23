import { MimeType } from "@bluemind/email";
import { inject } from "@bluemind/inject";
import ItemUri from "@bluemind/item-uri";

import mutationTypes from "../../store/mutationTypes";

export function selectMessage({ dispatch, commit, state, rootState, rootGetters }, messageKey) {
    const CAPABILITIES = [MimeType.TEXT_HTML, MimeType.TEXT_PLAIN];

    if (rootState.mail.activeFolder && !ItemUri.isItemUri(messageKey)) {
        messageKey = ItemUri.encode(parseInt(messageKey), rootState.mail.activeFolder);
    }

    if (state.currentMessage.key !== messageKey) {
        return dispatch("$_getIfNotPresent", [messageKey]).then(messages => {
            const message = messages[0];
            const promises = [];
            commit("currentMessage/update", { key: messageKey });

            if (inject("UserSession").roles.includes("hasCalendar") && !message.ics.isEmpty) {
                promises.push(dispatch("mail/FETCH_EVENT", message.ics.eventUid, { root: true }));
            }
            if (ItemUri.container(messageKey) === rootGetters["mail/MY_DRAFTS"].key) {
                commit("mail/" + mutationTypes.SET_MESSAGE_COMPOSING, { messageKey, composing: true }, { root: true });
            } else {
                // FIXME: remove me once MailViewer manage parts fetch
                const parts = message.computeParts();
                const inlines = parts.inlines.find(part =>
                    part.capabilities.every(capability => CAPABILITIES.includes(capability))
                );
                commit("currentMessage/setParts", { attachments: parts.attachments, inlines: inlines.parts });
                promises.push(
                    ...inlines.parts.map(part => dispatch("messages/fetch", { messageKey, part, isAttachment: false }))
                );
                return Promise.all(promises);
            }
        });
    }
    return Promise.resolve();
}
