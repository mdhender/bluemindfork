import { ContactValidator } from "@bluemind/contact";
import { Flag } from "@bluemind/email";
import { inject } from "@bluemind/inject";
import { retrieveTaskResult } from "@bluemind/task";
import { folderUtils, messageUtils } from "@bluemind/mail";

import { ADD_FLAG, SAVE_MESSAGE } from "~/actions";
import { REMOVE_MESSAGES, SET_MESSAGES_STATUS } from "~/mutations";

const { MessageAdaptor, MessageStatus, MessageHeader, MessageCreationModes } = messageUtils;

/** Send the last draft: move it to the Outbox then flush. */
export default async function (context, { draftKey, myMailboxKey, outbox, myDraftsFolder, messageCompose, files }) {
    const draft = context.state[draftKey];

    await context.dispatch(SAVE_MESSAGE, { draft, messageCompose, files });

    context.commit(SET_MESSAGES_STATUS, [{ key: draftKey, status: MessageStatus.SENDING }]);

    validateDraft(draft, inject("i18n"));
    const messageInOutboxId = await moveToOutbox(
        draft.remoteRef.internalId,
        myMailboxKey,
        outbox.remoteRef.internalId,
        myDraftsFolder.remoteRef.internalId
    );
    context.commit(REMOVE_MESSAGES, { messages: [draft] });
    const taskResult = await flush(); // flush means send mail + move to sentbox

    manageFlagOnPreviousMessage(context, draft);
    removeAttachmentAndInlineTmpParts(draft, messageCompose);

    return await getSentMessage(taskResult, messageInOutboxId, outbox);
}

function removeAttachmentAndInlineTmpParts(draft, messageCompose) {
    const service = inject("MailboxItemsPersistence", draft.folderRef.uid);
    const addresses = draft.attachments.concat(messageCompose.inlineImagesSaved).map(part => part.address);
    addresses.forEach(address => service.removePart(address));
}

async function getSentMessage(taskResult, messageInOutboxId, outbox) {
    const flushResult = taskResult.result.find(
        r =>
            r.sourceFolderUid.toUpperCase() === outbox.remoteRef.uid.toUpperCase() &&
            r.sourceInternalId === messageInOutboxId
    );
    const mailItem = await inject("MailboxItemsPersistence", flushResult.destinationFolderUid).getCompleteById(
        flushResult.destinationInternalId
    );
    return MessageAdaptor.fromMailboxItem(mailItem, {
        key: flushResult.destinationFolderUid,
        uid: flushResult.destinationFolderUid
    });
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
        const folderKey = folderUtils.generateKey(folderUid);
        const messageKey = messageUtils.messageKey(messageInternalId, folderKey);
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
    const allRecipientsAreValid = recipients.every(ContactValidator.validateContact);
    if (!allRecipientsAreValid) {
        throw vueI18n.t("mail.error.email.address.invalid");
    }
}
