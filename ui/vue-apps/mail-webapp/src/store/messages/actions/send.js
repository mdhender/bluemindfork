import { EmailValidator, Flag } from "@bluemind/email";
import { inject } from "@bluemind/inject";

import actionTypes from "../../actionTypes";
import mutationTypes from "../../mutationTypes";
import MessageStatus from "../MessageStatus";

/** Send the last draft: move it to the Outbox then flush. */
export default async function (
    { commit, dispatch, state },
    { userPrefTextOnly, draftKey, myMailboxKey, outboxId, myDraftsFolder, sentFolder, editorContent }
) {
    const draft = state[draftKey];

    try {
        let draftId = await dispatch(actionTypes.SAVE_MESSAGE, {
            userPrefTextOnly,
            draftKey,
            myDraftsFolderKey: myDraftsFolder.key,
            editorContent
        });
        commit(mutationTypes.SET_MESSAGES_STATUS, [{ key: draftKey, status: MessageStatus.SENDING }]);
        validateDraft(draft);

        const moveResult = await moveToOutbox(draftId, myMailboxKey, outboxId, myDraftsFolder.id);
        if (!moveResult || moveResult.status !== "SUCCESS") {
            throw "Unable to move draft to Outbox.";
        }

        draftId = moveResult.doneIds[0].destination;
        const taskResult = await flush(); // flush means send mail + move to sentbox
        const mailItem = await getSentMessageId(taskResult, draftId, sentFolder.uid);

        clearAttachmentParts(myDraftsFolder.uid, draft);
        manageFlagOnPreviousMessage(draft, dispatch);

        commit(mutationTypes.SET_MESSAGES_STATUS, [{ key: draftKey, status: MessageStatus.LOADED }]);
        return mailItem;
    } catch (reason) {
        console.log(reason);
        commit(mutationTypes.SET_MESSAGES_STATUS, [{ key: draftKey, status: MessageStatus.SEND_ERROR }]);
        throw reason;
    }
}

function getSentMessageId(taskResult, draftId, sentboxUid) {
    if (taskResult.result && Array.isArray(taskResult.result)) {
        let importedMailboxItem = taskResult.result.find(r => r.source === draftId);
        return inject("MailboxItemsPersistence", sentboxUid).getCompleteById(importedMailboxItem.destination);
    } else {
        throw "Unable to retrieve task result";
    }
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

function moveToOutbox(draftId, myMailboxKey, outboxId, myDraftsFolderId) {
    return inject("MailboxFoldersPersistence", myMailboxKey).importItems(outboxId, {
        mailboxFolderId: myDraftsFolderId,
        ids: [{ id: draftId }],
        expectedIds: undefined,
        deleteFromSource: true
    });
}

function clearAttachmentParts(draftboxUid, draft) {
    const service = inject("MailboxItemsPersistence", draftboxUid);
    return Promise.all(
        draft.attachments
            .filter(a => draft.attachmentStatuses[a.uid] !== "ERROR")
            .map(a => service.removePart(a.address))
    );
}

function manageFlagOnPreviousMessage(draft, dispatch) {
    // FIXME
    if (draft.previousMessage && draft.previousMessage.action) {
        let action = draft.previousMessage.action;
        let mailboxItemFlag;
        if (action === "replyAll" || action === "reply") {
            mailboxItemFlag = Flag.ANSWERED;
        } else if (action === "forward") {
            mailboxItemFlag = Flag.FORWARDED;
        }
        dispatch(actionTypes.ADD_FLAG, { messageKeys: [draft.previousMessage.messageKey], flag: mailboxItemFlag });
    }
}

function validateDraft(draft) {
    let recipients = draft.to.concat(draft.cc).concat(draft.bcc);
    const allRecipientsAreValid = recipients.some(recipient => EmailValidator.validateAddress(recipient));
    if (!allRecipientsAreValid) {
        throw inject("i18n").t("mail.error.email.address.invalid");
    }
}

/**
 * Wait for the task to be finished or a timeout is reached.
 */
function retrieveTaskResult(taskService, delayTime = 500, maxTries = 60, iteration = 1) {
    return new Promise(resolve => setTimeout(() => resolve(taskService.status()), delayTime)).then(taskStatus => {
        const taskEnded =
            taskStatus && taskStatus.state && taskStatus.state !== "InProgress" && taskStatus.state !== "NotStarted";
        if (taskEnded) {
            return JSON.parse(taskStatus.result);
        } else {
            if (iteration < maxTries) {
                return retrieveTaskResult(taskService, delayTime, maxTries, ++iteration);
            } else {
                return Promise.reject("Timeout while retrieving task result");
            }
        }
    });
}
