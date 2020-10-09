import { inject } from "@bluemind/inject";
import ItemUri from "@bluemind/item-uri";

import mutationTypes from "../../store/mutationTypes";

export function selectMessage({ dispatch, commit, state, rootState, rootGetters }, messageKey) {
    if (rootState.mail.activeFolder && !ItemUri.isItemUri(messageKey)) {
        messageKey = ItemUri.encode(parseInt(messageKey), rootState.mail.activeFolder);
    }

    if (state.currentMessage.key !== messageKey) {
        if (rootState.mail.messages[messageKey].composing) {
            commit("currentMessage/update", { key: messageKey });
        } else {
            return dispatch("$_getIfNotPresent", [messageKey]).then(messages => {
                const message = messages[0];
                const promises = [];
                commit("currentMessage/update", { key: messageKey });

                if (inject("UserSession").roles.includes("hasCalendar") && !message.ics.isEmpty) {
                    promises.push(dispatch("mail/FETCH_EVENT", message.ics.eventUid, { root: true }));
                }

                if (ItemUri.container(messageKey) === rootGetters["mail/MY_DRAFTS"].key) {
                    commit(
                        "mail/" + mutationTypes.SET_MESSAGE_COMPOSING,
                        { messageKey, composing: true },
                        { root: true }
                    );
                }
            });
        }
    }
    return Promise.resolve();
}
