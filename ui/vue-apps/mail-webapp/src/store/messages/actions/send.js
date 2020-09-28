import { EmailValidator, Flag } from "@bluemind/email";
import { inject } from "@bluemind/inject";
import ItemUri from "@bluemind/item-uri";

import actionTypes from "../../actionTypes";
import mutationTypes from "../../mutationTypes";
import { MessageStatus, MessageHeader, MessageCreationModes } from "../../../model/message";

/** Send the last draft: move it to the Outbox then flush. */
export default async function (
    { commit, dispatch, state },
    { userPrefTextOnly, draftKey, myMailboxKey, outboxId, myDraftsFolder, sentFolder, messageCompose }
) {
    const draft = state[draftKey];
    let draftId = draft.remoteRef.internalId;

    await dispatch(actionTypes.SAVE_MESSAGE, {
        userPrefTextOnly,
        draftKey,
        myDraftsFolderKey: myDraftsFolder.key,
        messageCompose
    });

    commit(mutationTypes.SET_MESSAGES_STATUS, [{ key: draftKey, status: MessageStatus.SENDING }]);
    validateDraft(draft);

    const moveResult = await moveToOutbox(draftId, myMailboxKey, outboxId, myDraftsFolder.remoteRef.internalId);
    if (!moveResult || moveResult.status !== "SUCCESS") {
        throw "Unable to move draft to Outbox.";
    }

    draftId = moveResult.doneIds[0].destination;
    const taskResult = await flush(); // flush means send mail + move to sentbox
    const mailItem = await getSentMessageId(taskResult, draftId, sentFolder.remoteRef.uid);

    commit(mutationTypes.SET_MESSAGES_STATUS, [{ key: draftKey, status: MessageStatus.LOADED }]);

    manageFlagOnPreviousMessage(draft, dispatch, state);

    // add necessary data to build a link to the newly created message in Sent box
    mailItem.subjectLink = {
        name: "v:mail:message",
        params: { message: ItemUri.encode(mailItem.internalId, sentFolder.remoteRef.uid), folder: sentFolder.path }
    };

    return mailItem;
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

function manageFlagOnPreviousMessage(draft, dispatch, state) {
    const draftInfoHeader = draft.headers.find(header => header.name === MessageHeader.X_BM_DRAFT_INFO);
    if (draftInfoHeader) {
        const [typeOfDraft, messageInternalId, folderUid] = draftInfoHeader.values[0].split(",");
        const mailboxItemFlag = typeOfDraft === MessageCreationModes.FORWARD ? Flag.FORWARDED : Flag.ANSWERED;
        const messageKey = ItemUri.encode(parseInt(messageInternalId), folderUid);
        if (state[messageKey]) {
            dispatch(actionTypes.ADD_FLAG, {
                messageKeys: [messageKey],
                flag: mailboxItemFlag
            });
        } else {
            inject("MailboxItemsPersistence", folderUid).addFlag({ itemsId: [messageInternalId], mailboxItemFlag });
        }
    }
}

function validateDraft(draft) {
    let recipients = draft.to.concat(draft.cc).concat(draft.bcc);
    const allRecipientsAreValid = recipients.some(recipient => EmailValidator.validateAddress(recipient.address));
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
