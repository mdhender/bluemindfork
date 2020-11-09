import { ItemUri } from "@bluemind/item-uri";

import { createOnlyMetadata } from "../../model/message";
import { MESSAGE_IS_LOADED } from "~getters";
import { ADD_MESSAGES } from "~mutations";

export async function $_getIfNotPresent({ dispatch, commit, rootState, rootGetters }, messageKeys) {
    const missingMetadata = messageKeys
        .filter(key => !rootState.mail.messages[key])
        .map(key => {
            const [internalId, folderKey] = ItemUri.decode(key);
            return createOnlyMetadata({ internalId, folder: { key: folderKey, uid: folderKey } });
        });
    commit("mail/" + ADD_MESSAGES, missingMetadata, { root: true });

    const missingData = messageKeys.filter(messageKey => !rootGetters["mail/" + MESSAGE_IS_LOADED](messageKey));
    await dispatch("messages/multipleByKey", missingData);
    return messageKeys.map(key => rootState.mail.messages[key]);
}
