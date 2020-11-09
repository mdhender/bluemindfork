import { inject } from "@bluemind/inject";

import { AttachmentStatus } from "../../../model/attachment";
import { SAVE_MESSAGE } from "~actions";
import { REMOVE_ATTACHMENT } from "~mutations";

export default async function (
    { commit, dispatch, state },
    { messageKey, attachmentAddress, userPrefTextOnly, myDraftsFolderKey, messageCompose }
) {
    const message = state[messageKey];
    const status = message.attachments.find(attachment => attachment.address === attachmentAddress).status;

    if (status !== AttachmentStatus.ERROR) {
        await inject("MailboxItemsPersistence", message.folderRef.uid).removePart(attachmentAddress);
    }
    commit(REMOVE_ATTACHMENT, { messageKey, address: attachmentAddress });

    await dispatch(SAVE_MESSAGE, {
        userPrefTextOnly,
        draftKey: messageKey,
        myDraftsFolderKey,
        messageCompose
    });
}
