import { ItemUri } from "@bluemind/item-uri";

import { createOnlyMetadata } from "../../model/message";
import mutationTypes from "../../store/mutationTypes";

export function $_getIfNotPresent({ dispatch, getters, commit, rootState, rootGetters }, messageKeys) {
    const missingMetadata = messageKeys
        .filter(key => !rootState.mail.messages[key])
        .map(key => {
            const [internalId, folderKey] = ItemUri.decode(key);
            return createOnlyMetadata({ internalId, folder: { key: folderKey, uid: folderKey } });
        });
    commit("mail/" + mutationTypes.ADD_MESSAGES, missingMetadata, { root: true });

    const missingData = messageKeys.filter(messageKey => !rootGetters["mail/isLoaded"](messageKey));
    return dispatch("messages/multipleByKey", missingData).then(() =>
        getters["messages/getMessagesByKey"](messageKeys)
    );
}
