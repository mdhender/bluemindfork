import { DEBOUNCED_SAVE_MESSAGE } from "~actions";
import { REMOVE_ATTACHMENT, SET_MESSAGE_HAS_ATTACHMENT } from "~mutations";

export default async function ({ commit, dispatch, state }, { messageKey, attachmentAddress, messageCompose }) {
    const draft = state[messageKey];

    commit(REMOVE_ATTACHMENT, { messageKey, address: attachmentAddress });
    commit(SET_MESSAGE_HAS_ATTACHMENT, {
        key: messageKey,
        hasAttachment: draft.attachments.length > 0
    });

    dispatch(DEBOUNCED_SAVE_MESSAGE, { draft, messageCompose });
}
