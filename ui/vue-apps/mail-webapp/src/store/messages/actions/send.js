import { EmailValidator, Flag } from "@bluemind/email";
import { inject } from "@bluemind/inject";
import { retrieveTaskResult } from "@bluemind/task";

import { ADD_FLAG, REPLACE_DRAFT_MESSAGE, SAVE_MESSAGE } from "~/actions";
import { MessageStatus, MessageHeader, MessageCreationModes } from "~/model/message";
import { SET_MESSAGES_STATUS } from "~/mutations";
import MessageAdaptor from "../helpers/MessageAdaptor";
import { FolderAdaptor } from "../../folders/helpers/FolderAdaptor";
import * as Folder from "~/model/folder";
import * as Message from "~/model/message";

/** Send the last draft: move it to the Outbox then flush. */
export default async function (
    context,
    { draftKey, myMailboxKey, outboxId, myDraftsFolder, sentFolder, messageCompose }
) {
    const draft = context.state[draftKey];

    await context.dispatch(SAVE_MESSAGE, { draft, messageCompose });

    let draftId = draft.remoteRef.internalId;

    context.commit(SET_MESSAGES_STATUS, [{ key: draftKey, status: MessageStatus.SENDING }]);
    validateDraft(draft, inject("i18n"));
    draftId = await moveToOutbox(draftId, myMailboxKey, outboxId, myDraftsFolder.remoteRef.internalId);
    const taskResult = await flush(); // flush means send mail + move to sentbox
    context.commit(SET_MESSAGES_STATUS, [{ key: draftKey, status: MessageStatus.SENT }]);

    manageFlagOnPreviousMessage(context, draft);
    removeAttachmentAndInlineTmpParts(draft, messageCompose);

    const message = await getSentMessage(taskResult, draftId, sentFolder);
    message.conversationRef = draft.conversationRef;
    await context.dispatch(REPLACE_DRAFT_MESSAGE, { draft, message });
    return message;
}

function removeAttachmentAndInlineTmpParts(draft, messageCompose) {
    const service = inject("MailboxItemsPersistence", draft.folderRef.uid);
    const addresses = draft.attachments.concat(messageCompose.inlineImagesSaved).map(part => part.address);
    addresses.forEach(address => service.removePart(address));
}

async function getSentMessage(taskResult, draftId, sentFolder) {
    let mailItem;
    if (taskResult.result && Array.isArray(taskResult.result)) {
        let importedMailboxItem = taskResult.result.find(r => r.source === draftId);
        mailItem = await inject("MailboxItemsPersistence", sentFolder.remoteRef.uid).getCompleteById(
            importedMailboxItem.destination
        );
    } else {
        throw "Unable to retrieve task result";
    }
    return MessageAdaptor.fromMailboxItem(mailItem, FolderAdaptor.toRef(sentFolder));
}

function flush() {
    return inject("OutboxPersistence")
        .flush()
        .then(taskRef => {
            // wait for the flush of the outbox to be finished
            const taskService = inject("TaskService", taskRef.id);
            return retrieveTaskResult(taskService);
        });
}

async function moveToOutbox(draftId, myMailboxKey, outboxId, myDraftsFolderId) {
    const moveResult = await inject("MailboxFoldersPersistence", myMailboxKey).importItems(outboxId, {
        mailboxFolderId: myDraftsFolderId,
        ids: [{ id: draftId }],
        expectedIds: undefined,
        deleteFromSource: true
    });
    if (!moveResult || moveResult.status !== "SUCCESS") {
        throw "Unable to move draft to Outbox.";
    }
    return moveResult.doneIds[0].destination;
}

function manageFlagOnPreviousMessage({ dispatch, state }, draft) {
    const hasDraftInfoHeader = draft.headers.find(header => header.name === MessageHeader.X_BM_DRAFT_INFO);
    if (hasDraftInfoHeader) {
        const draftInfoHeader = JSON.parse(hasDraftInfoHeader.values[0]);
        const mailboxItemFlag = draftInfoHeader.type === MessageCreationModes.FORWARD ? Flag.FORWARDED : Flag.ANSWERED;

        const messageInternalId = draftInfoHeader.messageInternalId;
        const folderUid = draftInfoHeader.folderUid;
        const folderKey = Folder.generateKey(folderUid);
        const messageKey = Message.messageKey(messageInternalId, folderKey);
        const message = state[messageKey];
        if (message) {
            dispatch(ADD_FLAG, {
                messages: [message],
                flag: mailboxItemFlag
            });
        } else {
            inject("MailboxItemsPersistence", folderUid).addFlag({ itemsId: [messageInternalId], mailboxItemFlag });
        }
    }
}

function validateDraft(draft, vueI18n) {
    let recipients = draft.to.concat(draft.cc).concat(draft.bcc);
    const allRecipientsAreValid = recipients.some(recipient => EmailValidator.validateAddress(recipient.address));
    if (!allRecipientsAreValid) {
        throw vueI18n.t("mail.error.email.address.invalid");
    }
}
