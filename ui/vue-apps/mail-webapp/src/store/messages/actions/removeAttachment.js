import { inject } from "@bluemind/inject";

import actionTypes from "../../actionTypes";
import mutationTypes from "../../mutationTypes";
import { AttachmentStatus } from "../../../model/attachment";

export default async function (
    { commit, dispatch, getters, state },
    { messageKey, attachmentAddress, userPrefTextOnly, myDraftsFolderKey, editorContent }
) {
    const status = state[messageKey].attachments.find(attachment => attachment.address === attachmentAddress).status;

    if (status !== AttachmentStatus.ERROR) {
        await inject("MailboxItemsPersistence", getters["MY_DRAFTS"].uid).removePart(attachmentAddress);
    }
    commit(mutationTypes.REMOVE_ATTACHMENT, { messageKey, address: attachmentAddress });
    await dispatch(actionTypes.SAVE_MESSAGE, {
        userPrefTextOnly,
        draftKey: messageKey,
        myDraftsFolderKey,
        editorContent
    });
}
