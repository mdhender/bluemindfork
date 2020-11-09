import { inject } from "@bluemind/inject";
import ItemUri from "@bluemind/item-uri";

import { MessageHeader } from "../../model/message";
import { CURRENT_MAILBOX, MY_DRAFTS } from "~getters";
import { FETCH_EVENT } from "~actions";
import { SET_MESSAGE_COMPOSING } from "~mutations";

export async function selectMessage({ dispatch, commit, state, rootState, rootGetters }, messageKey) {
    if (rootState.mail.activeFolder && !ItemUri.isItemUri(messageKey)) {
        messageKey = ItemUri.encode(parseInt(messageKey), rootState.mail.activeFolder);
    }

    if (state.currentMessage.key !== messageKey) {
        await dispatch("$_getIfNotPresent", [messageKey]);
        const message = rootState.mail.messages[messageKey];

        if (!message.composing) {
            if (inject("UserSession").roles.includes("hasCalendar") && message.hasICS) {
                const icsHeaderValue = message.headers.find(header => header.name === MessageHeader.X_BM_EVENT)
                    .values[0];
                const semiColonIndex = icsHeaderValue.indexOf(";");
                const eventUid = semiColonIndex === -1 ? icsHeaderValue : icsHeaderValue.substring(0, semiColonIndex);
                await dispatch(
                    "mail/" + FETCH_EVENT,
                    { eventUid, mailbox: rootGetters["mail/" + CURRENT_MAILBOX] },
                    { root: true }
                );
            }

            if (ItemUri.container(messageKey) === rootGetters["mail/" + MY_DRAFTS].key) {
                commit("mail/" + SET_MESSAGE_COMPOSING, { messageKey, composing: true }, { root: true });
            }
        }

        commit("currentMessage/update", { key: messageKey });
    }
}
