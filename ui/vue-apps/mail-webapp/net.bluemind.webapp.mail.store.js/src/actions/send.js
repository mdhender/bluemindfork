//FIXME: Refactor this...

import { DraftStatus } from "@bluemind/backend.mail.store";
import { EmailValidator } from "@bluemind/email";
import injector from "@bluemind/inject";
import UUIDGenerator from "@bluemind/uuid";
import ItemUri from "@bluemind/item-uri";

/** Send the last draft: move it to the Outbox then flush. */
export function send({ state, commit, getters, dispatch }) {
    const draft = state.draft;
    let draftId = draft.id,
        sentbox;
    const loadingAlertUid = UUIDGenerator.generate();

    commit(
        "alert/add",
        {
            code: "MSG_SEND_LOADING",
            uid: loadingAlertUid,
            props: { subject: draft.subject }
        },
        { root: true }
    );

    // ensure the last draft is up to date
    return dispatch("saveDraft")
        .then(newDraftId => {
            draftId = newDraftId;
            commit("updateDraft", { status: DraftStatus.SENDING });

            // validation
            if (!validate(draft)) {
                throw injector
                    .getProvider("i18n")
                    .get()
                    .t("mail.error.email.address.invalid");
            }
            return Promise.resolve();
        })
        .then(() => {
            // move draft from draftbox to outbox
            const draftbox = getters.my.DRAFTS;
            const outbox = getters.my.OUTBOX;
            return injector
                .getProvider("MailboxFoldersPersistence")
                .get(getters.my.mailboxUid)
                .importItems(outbox.internalId, {
                    mailboxFolderId: draftbox.internalId,
                    ids: [{ id: draftId }],
                    expectedIds: undefined,
                    deleteFromSource: true
                });
        })
        .then(moveResult => {
            // flush the outbox
            if (!moveResult || moveResult.status !== "SUCCESS") {
                throw "Unable to flush the Outbox.";
            }
            draftId = moveResult.doneIds[0].destination;
            const outboxService = injector.getProvider("OutboxPersistence").get();
            return outboxService.flush();
        })
        .then(taskRef => {
            // wait for the flush of the outbox to be finished (flush means send mail + move to sentbox)
            const taskService = injector.getProvider("TaskService").get(taskRef.id);
            return retrieveTaskResult(taskService, 250, 5);
        })
        .then(taskResult => {
            // compute and return the IMAP id of the mail inside the sentbox
            if (taskResult.result && Array.isArray(taskResult.result)) {
                let importedMailboxItem = taskResult.result.find(r => r.source === draftId);
                sentbox = getters.my.SENT;
                const sentboxItemsService = injector.getProvider("MailboxItemsPersistence").get(sentbox.uid);
                return sentboxItemsService.getCompleteById(importedMailboxItem.destination);
            } else {
                throw "Unable to retrieve task result";
            }
        })
        .then(mailItem => {
            const messageKey = ItemUri.encode(mailItem.internalId, getters.my.SENT.uid);
            commit("alert/remove", loadingAlertUid, { root: true });
            commit(
                "alert/add",
                {
                    code: "MSG_SENT_OK",
                    props: {
                        subject: draft.subject,
                        subjectLink: "/mail/" + sentbox.key + "/" + messageKey
                    }
                },
                { root: true }
            );
            commit("updateDraft", { status: DraftStatus.SENT, id: null, saveDate: null });
        })
        .catch(reason => {
            commit("alert/remove", loadingAlertUid, { root: true });
            commit(
                "alert/add",
                {
                    code: "MSG_SENT_ERROR",
                    props: { subject: draft.subject, reason: reason.message }
                },
                { root: true }
            );
            commit("updateDraft", { status: DraftStatus.SENT });
        });
}

function validate(messageToSend) {
    let recipients = messageToSend.to.concat(messageToSend.cc).concat(messageToSend.bcc);
    return recipients.some(recipient => EmailValidator.validateAddress(recipient));
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
