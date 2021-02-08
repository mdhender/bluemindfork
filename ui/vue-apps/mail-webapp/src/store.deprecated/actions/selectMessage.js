import { inject } from "@bluemind/inject";
import ItemUri from "@bluemind/item-uri";

import { CURRENT_MAILBOX, MY_DRAFTS } from "~getters";
import { FETCH_EVENT } from "~actions";
import { RESET_ACTIVE_MESSAGE, SET_MESSAGE_COMPOSING, SET_ACTIVE_FOLDER } from "~mutations";

export async function selectMessage({ dispatch, commit, state, rootState, rootGetters }, messageKey) {
    if (rootState.mail.activeFolder && !ItemUri.isItemUri(messageKey)) {
        messageKey = ItemUri.encode(parseInt(messageKey), rootState.mail.activeFolder);
    }

    if (state.currentMessage.key !== messageKey) {
        await dispatch("$_getIfNotPresent", [messageKey]);
        const message = rootState.mail.messages[messageKey];
        if (!message.composing) {
            const folder = rootState.mail.folders[message.folderRef.key];
            commit("mail/" + SET_ACTIVE_FOLDER, folder, { root: true });
            if (inject("UserSession").roles.includes("hasCalendar") && message.hasICS) {
                await dispatch(
                    "mail/" + FETCH_EVENT,
                    { message, mailbox: rootGetters["mail/" + CURRENT_MAILBOX] },
                    { root: true }
                );
            }

            if (ItemUri.container(messageKey) === rootGetters["mail/" + MY_DRAFTS].key) {
                commit("mail/" + SET_MESSAGE_COMPOSING, { messageKey, composing: true }, { root: true });
            }
        }
        commit("mail/" + RESET_ACTIVE_MESSAGE, null, { root: true });
        commit("currentMessage/update", { key: messageKey });
    }
}
