import { inject } from "@bluemind/inject";

import actionTypes from "../../actionTypes";
import mutationTypes from "../../mutationTypes";
import { AttachmentStatus } from "../../../model/attachment";

export default async function (
    { commit, dispatch, state },
    { messageKey, attachmentAddress, userPrefTextOnly, myDraftsFolderKey, messageCompose }
) {
    const status = state[messageKey].attachments.find(attachment => attachment.address === attachmentAddress).status;

    if (status !== AttachmentStatus.ERROR) {
        await inject("MailboxItemsPersistence", state[messageKey].folderRef.uid).removePart(attachmentAddress);
    }
    commit(mutationTypes.REMOVE_ATTACHMENT, { messageKey, address: attachmentAddress });
    await dispatch(actionTypes.SAVE_MESSAGE, {
        userPrefTextOnly,
        draftKey: messageKey,
        myDraftsFolderKey,
        messageCompose
    });
}
