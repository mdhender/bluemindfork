import { inject } from "@bluemind/inject";

import { DEBOUNCED_SAVE_MESSAGE } from "~/actions";
import { REMOVE_ATTACHMENT, REMOVE_FILE, SET_MESSAGE_HAS_ATTACHMENT } from "~/mutations";

export default async function ({ commit, dispatch, state }, { messageKey, attachment, messageCompose }) {
    const draft = state[messageKey];
    commit(REMOVE_FILE, { key: attachment.fileKey });
    commit(REMOVE_ATTACHMENT, { messageKey, address: attachment.address });
    commit(SET_MESSAGE_HAS_ATTACHMENT, {
        key: messageKey,
        hasAttachment: draft.attachments.length > 0
    });

    dispatch(DEBOUNCED_SAVE_MESSAGE, { draft, messageCompose });

    inject("MailboxItemsPersistence", draft.folderRef.uid).removePart(attachment.address);
}
