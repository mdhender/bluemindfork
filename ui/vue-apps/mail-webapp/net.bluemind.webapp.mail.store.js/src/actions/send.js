import { DraftStatus } from "@bluemind/backend.mail.store";
import { EmailValidator, Flag } from "@bluemind/email";
import injector from "@bluemind/inject";
import UUIDGenerator from "@bluemind/uuid";
import ItemUri from "@bluemind/item-uri";

/** Send the last draft: move it to the Outbox then flush. */
export function send({ state, commit, getters, dispatch }) {
    const draft = state.draft;
    let draftId = draft.id;
    const loadingAlertUid = addLoadingAlert(commit, draft.subject);

    return dispatch("saveDraft")
        .then(newDraftId => {
            draftId = newDraftId;
            commit("draft/update", { status: DraftStatus.SENDING });
            validateDraft(draft);
            return moveToOutbox(getters.my, draftId);
        })
        .then(moveResult => {
            if (!moveResult || moveResult.status !== "SUCCESS") {
                throw "Unable to flush the Outbox.";
            }
            draftId = moveResult.doneIds[0].destination;
            return flush(); // flush means send mail + move to sentbox
        })
        .then(taskResult => getSentMessageId(taskResult, draftId, getters.my.SENT.uid))
        .then(mailItem => handleSuccess(mailItem.internalId, loadingAlertUid, getters.my, draft, commit, dispatch))
        .catch(reason => handleError(commit, draft.subject, loadingAlertUid, reason));
}

function getSentMessageId(taskResult, draftId, sentboxUid) {
    if (taskResult.result && Array.isArray(taskResult.result)) {
        let importedMailboxItem = taskResult.result.find(r => r.source === draftId);
        return injector
            .getProvider("MailboxItemsPersistence")
            .get(sentboxUid)
            .getCompleteById(importedMailboxItem.destination);
    } else {
        throw "Unable to retrieve task result";
    }
}

function flush() {
    return injector
        .getProvider("OutboxPersistence")
        .get()
        .flush()
        .then(taskRef => {
            // wait for the flush of the outbox to be finished
            const taskService = injector.getProvider("TaskService").get(taskRef.id);
            return retrieveTaskResult(taskService, 250, 5);
        });
}

function moveToOutbox(my, draftId) {
    return injector
        .getProvider("MailboxFoldersPersistence")
        .get(my.mailboxUid)
        .importItems(my.OUTBOX.internalId, {
            mailboxFolderId: my.DRAFTS.internalId,
            ids: [{ id: draftId }],
            expectedIds: undefined,
            deleteFromSource: true
        });
}

function handleSuccess(sentMailId, loadingAlertUid, my, draft, commit, dispatch) {
    clearAttachmentParts(my.DRAFTS.uid, draft);
    manageFlagOnPreviousMessage(draft, dispatch);
    const messageKey = ItemUri.encode(sentMailId, my.SENT.uid);
    commit("removeApplicationAlert", loadingAlertUid, { root: true });
    commit(
        "addApplicationAlert",
        {
            code: "MSG_SENT_OK",
            props: {
                subject: draft.subject,
                subjectLink: {
                    name: "v:mail:message",
                    params: { message: messageKey, folder: my.SENT.value.fullName }
                }
            }
        },
        { root: true }
    );
    commit("draft/update", { status: DraftStatus.SENT, id: null, saveDate: null });
}

function clearAttachmentParts(draftboxUid, draft) {
    const service = injector.getProvider("MailboxItemsPersistence").get(draftboxUid);
    return Promise.all(
        draft.parts.attachments
            .filter(a => draft.attachmentStatuses[a.uid] !== "ERROR")
            .map(a => service.removePart(a.address))
    );
}

function manageFlagOnPreviousMessage(draft, dispatch) {
    if (draft.previousMessage && draft.previousMessage.action) {
        let action = draft.previousMessage.action;
        let mailboxItemFlag;
        if (action === "replyAll" || action === "reply") {
            mailboxItemFlag = Flag.ANSWERED;
        } else if (action === "forward") {
            mailboxItemFlag = Flag.FORWARDED;
        }
        dispatch("messages/addFlag", { messageKeys: [draft.previousMessage.messageKey], mailboxItemFlag });
    }
}

function handleError(commit, draftSubject, loadingAlertUid, reason) {
    commit("removeApplicationAlert", loadingAlertUid, { root: true });
    commit(
        "addApplicationAlert",
        {
            code: "MSG_SENT_ERROR",
            props: { subject: draftSubject, reason: reason.message }
        },
        { root: true }
    );
    commit("draft/update", { status: DraftStatus.SENT });
}

function addLoadingAlert(commit, draftSubject) {
    const loadingAlertUid = UUIDGenerator.generate();

    commit(
        "addApplicationAlert",
        {
            code: "MSG_SEND_LOADING",
            uid: loadingAlertUid,
            props: { subject: draftSubject }
        },
        { root: true }
    );

    return loadingAlertUid;
}

function validateDraft(draft) {
    let recipients = draft.to.concat(draft.cc).concat(draft.bcc);
    const allRecipientsAreValid = recipients.some(recipient => EmailValidator.validateAddress(recipient));
    if (!allRecipientsAreValid) {
        throw injector
            .getProvider("i18n")
            .get()
            .t("mail.error.email.address.invalid");
    }
}

/** Wait for the task to be finished or a timeout is reached. */
function retrieveTaskResult(taskService, delayTime, maxTries, iteration = 1) {
    return new Promise(resolve => setTimeout(() => resolve(taskService.status()), delayTime)).then(taskStatus => {
        const taskEnded =
            taskStatus && taskStatus.state && taskStatus.state !== "InProgress" && taskStatus.state !== "NotStarted";
        if (taskEnded) {
            return JSON.parse(taskStatus.result);
        } else {
            if (iteration < maxTries) {
                // 'smart' delay: add 250ms each retry
                return retrieveTaskResult(taskService, delayTime + 250, maxTries, ++iteration);
            } else {
                return Promise.reject("Timeout while retrieving task result");
            }
        }
    });
}
