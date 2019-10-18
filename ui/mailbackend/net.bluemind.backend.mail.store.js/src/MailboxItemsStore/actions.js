import { EmailValidator, MimeType } from "@bluemind/email";
import injector from "@bluemind/inject";
import PartsHelper from "./PartsHelper";
import ServiceLocator from "@bluemind/inject";
import uuid from "uuid/v4";
import Message from "./Message.js";
import { getLocalizedProperty } from "@bluemind/backend.mail.l10n";
import { ItemFlag } from "@bluemind/core.container.api";
import { AlertTypes, Alert } from "@bluemind/alert.store";
import DraftStatus from "./DraftStatus";
import { html2text } from "@bluemind/html-utils";

export function $_mailrecord_changed({ dispatch }, { container }) {
    return dispatch("all", container);
}

export function all({ commit, dispatch }, folder) {
    const service = ServiceLocator.getProvider("MailboxItemsPersistance").get(folder);

    return service
        .filteredChangesetById(0, { must: [], mustNot: [ItemFlag.Deleted] })
        .then(changeset => changeset.created.map(itemVersion => itemVersion.id))
        .then(ids => {
            commit("setSortedIds", ids);
            return dispatch("multipleById", { folder, ids: ids.slice(0, 20) });
        })
        .then(() => service.getPerUserUnread())
        .then(unread => commit("setUnreadCount", unread.total));
}

export function multipleById({ commit }, { folder, ids }) {
    const service = ServiceLocator.getProvider("MailboxItemsPersistance").get(folder);
    return service.multipleById(ids).then(items => commit("setItems", items));
}

/* TODO remove when webapp store is ready : following calls should be done in webapp, not in mailbackend */
export function select({ state, commit, dispatch }, { folder, id }) {
    const payload = {};
    let promise = Promise.resolve();
    if (!state.items[id]) {
        promise = ServiceLocator.getProvider("MailboxItemsPersistance")
            .get(folder)
            .getCompleteById(id)
            .then(item => commit("setItems", [item]));
    }
    return promise
        .then(() => {
            commit("setCurrent", id);
            // 1) Select the current message to display

            // 2) Obtain the different display possibilities
            payload.result = visitParts(state, id);

            // 3) Choose parts to display
            payload.chosenParts = choosePartsToDisplay(payload.result.inlines);

            // 4) Retrieve parts content
            return fetchParts(state, commit, folder, id, dispatch, payload.chosenParts);
        })
        .then(() => {
            // 5) Manipulate parts content before display
            payload.chosenParts = processBeforeDisplay(payload.chosenParts);

            // 6) Add attachments
            commit("setAttachments", payload.result.attachments);

            return displayParts(commit, payload.chosenParts);
        });
}

/**
 * Obtain the different display possibilities.
 *
 * TODO move to webapp store or router when ready.
 */
function visitParts(state, id) {
    const message = new Message(state.items[id]);
    return message.computeParts();
}

/**
 * Chose what to display.
 *
 * TODO move to webapp store or router when ready.
 */
function choosePartsToDisplay(partsByCapabilities) {
    // we may have no choice (only one entry without capabilities)
    if (partsByCapabilities.length === 1) {
        return partsByCapabilities[0].parts;
    }

    // for now we take the last branch containing an HTML capacity
    let chosenPartsByCapabilities = partsByCapabilities.find(
        partsByCapabilities => partsByCapabilities.capabilities.indexOf(MimeType.TEXT_HTML) !== -1
    );
    if (!chosenPartsByCapabilities) {
        // fallback on PLAIN capacity if any
        chosenPartsByCapabilities = partsByCapabilities.find(
            partsByCapabilities => partsByCapabilities.capabilities.indexOf(MimeType.TEXT_PLAIN) !== -1
        );
    }
    return chosenPartsByCapabilities.parts;
}

/**
 * Retrieve parts content (as a Promise).
 *
 * TODO move to webapp store or router when ready.
 */
function fetchParts(state, commit, folder, id, dispatch, partsToFetch) {
    // use mailbackend to fetch parts
    let promises = partsToFetch.map(part => dispatch("fetch", { folder, id, part, isAttachment: false }));
    return Promise.all(promises).catch(reason => {
        commit("alert/addAlert", { uid: uuid(), type: "error", message: "Failed to read mail " + reason });
    });
}

/**
 * Manipulate parts content before display.
 *
 * TODO move to webapp store or router when ready.
 */
function processBeforeDisplay(fetchedParts) {
    const htmlParts = fetchedParts.filter(part => part.mime === "text/html");
    const imageParts = fetchedParts.filter(part => MimeType.isImage(part) && toBeIncluded(part));
    PartsHelper.insertInlineImages(htmlParts, imageParts);
    const otherParts = fetchedParts.filter(part => part.mime !== "text/html" && !toBeIncluded(part));
    return [...htmlParts, ...otherParts];
}

function toBeIncluded(part) {
    return part.cid;
}

/**
 * Render the parts.
 *
 * TODO move to webapp store or router when ready.
 */
function displayParts(commit, partsToDisplay) {
    commit("setPartsToDisplay", partsToDisplay);
}

export function fetch({ state }, { folder, id, part, isAttachment }) {
    const item = state.items[id];
    let encoding = part.encoding;
    if (isAttachment || MimeType.isImage(part)) {
        encoding = null;
    }
    return ServiceLocator.getProvider("MailboxItemsPersistance")
        .get(folder)
        .fetch(item.value.imapUid, part.address, encoding, part.mime, part.charset)
        .then(function (stream) {
            part.content = stream;
            part.id = id;
            part.cid = part.contentId;
        });
}

export function updateSeen({ commit, rootGetters }, { folder, id, isSeen }) {
    folder = folder || rootGetters["backend.mail/folders/currentFolder"];
    const service = ServiceLocator.getProvider("MailboxItemsPersistance").get(folder);
    return service
        .updateSeens([{ itemId: id, seen: isSeen, mdnSent: false }])
        .then(() => commit("updateSeen", { id, isSeen }))
        .then(() => service.getPerUserUnread())
        .then(unread => commit("setUnreadCount", unread.total));
}

/** Send the last draft: move it to the Outbox then flush. */
export function send({ state, commit, rootState, dispatch }) {
    const draft = state.draft;
    let draftId = draft.id;
    let userSession;
    let sentbox;

    // ensure the last draft is up to date
    return dispatch("saveDraft")
        .then(newDraftId => {
            draftId = newDraftId;
            commit("updateDraft", { status: DraftStatus.SENDING });

            // validation
            userSession = injector.getProvider("UserSession").get();
            if (!validate(draft)) {
                throw getLocalizedProperty(userSession, "mail.error.email.address.invalid");
            }
            return Promise.resolve();
        })
        .then(() => {
            // move draft from draftbox to outbox
            const draftbox = rootState["backend.mail/folders"].folders.find(function (folder) {
                return folder.displayName === "Drafts";
            });
            const outbox = rootState["backend.mail/folders"].folders.find(function (folder) {
                return folder.displayName === "Outbox";
            });
            return ServiceLocator.getProvider("MailboxFoldersPersistance")
                .get()
                .importItems(outbox.internalId, {
                    mailboxFolderId: draftbox.internalId,
                    ids: [{ id: draftId }],
                    expectedIds: undefined,
                    deleteFromSource: true
                });
        })
        .then(moveResult => {
            // flush the outbox
            if (!moveResult || moveResult.status != "SUCCESS") {
                throw "Unable to flush the Outbox.";
            }
            draftId = moveResult.doneIds[0].destination;
            const outboxService = ServiceLocator.getProvider("OutboxPersistance").get();
            return outboxService.flush();
        })
        .then(taskRef => {
            // wait for the flush of the outbox to be finished (flush means send mail + move to sentbox)
            const taskService = ServiceLocator.getProvider("TaskService").get(taskRef.id);
            return retrieveTaskResult(taskService, 250, 5);
        })
        .then(taskResult => {
            // compute and return the IMAP id of the mail inside the sentbox
            if (taskResult.result && Array.isArray(taskResult.result)) {
                let importedMailboxItem = taskResult.result.find(r => r.source == draftId);
                sentbox = rootState["backend.mail/folders"].folders.find(function (folder) {
                    return folder.displayName === "Sent";
                });
                const sentboxItemsService = ServiceLocator.getProvider("MailboxItemsPersistance").get(sentbox.uid);
                return sentboxItemsService.getCompleteById(importedMailboxItem.destination);
            } else {
                throw "Unable to retrieve task result";
            }
        })
        .then(mailItem => {
            const mailId = mailItem.internalId;
            const key = "mail.alert.message.sent.ok";
            const success = new Alert({
                type: AlertTypes.SUCCESS,
                code: "ALERT_CODE_MSG_SENT_OK",
                key,
                message: getLocalizedProperty(userSession, key, { subject: draft.subject }),
                props: {
                    subject: draft.subject,
                    subjectLink: "/mail/" + sentbox.uid + "/" + mailId
                }
            });
            commit("alert/addAlert", success, { root: true });
            commit("updateDraft", { status: DraftStatus.SENT, id: null, saveDate: null });
        })
        .catch(reason => {
            const key = "mail.alert.message.sent.error";
            const error = new Alert({
                code: "ALERT_CODE_MSG_SENT_ERROR",
                key,
                message: getLocalizedProperty(userSession, key, { subject: draft.subject, reason: reason.message }),
                props: { subject: draft.subject, reason: reason.message }
            });
            commit("alert/addAlert", error, { root: true });
            commit("updateDraft", { status: DraftStatus.SENT });
        });
}

/** Wait for the task to be finished or a timeout is reached. */
function retrieveTaskResult(taskService, delayTime, maxTries, iteration = 1) {
    return new Promise(resolve => setTimeout(() => resolve(taskService.status()), delayTime)).then(taskStatus => {
        const taskEnded =
            taskStatus && taskStatus.state && taskStatus.state != "InProgress" && taskStatus.state != "NotStarted";
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

function validate(messageToSend) {
    let recipients = messageToSend.to.concat(messageToSend.cc).concat(messageToSend.bcc);
    return recipients.some(recipient => EmailValidator.validateAddress(recipient));
}

function sanitize(messageToSend) {
    if (messageToSend.subject === "") {
        messageToSend.subject = "(No subject)";
    }
    messageToSend.content = messageToSend.content.replace(/[\n\r]/g, String.fromCharCode(13, 10));
}

const MAX_SEARCH_RESULTS = 500; // FIXME

export function search({ commit }, { folderUid, pattern }) {
    return ServiceLocator.getProvider("MailboxFoldersPersistance")
        .get()
        .searchItems({
            query: {
                searchSessionId: undefined,
                query: pattern,
                maxResults: MAX_SEARCH_RESULTS,
                offset: undefined,
                scope: {
                    folderScope: {
                        folderUid
                    },
                    isDeepTraversal: false
                }
            },
            sort: {
                criteria: [
                    {
                        field: "date",
                        order: "Desc"
                    }
                ]
            }
        })
        .then(searchResults => {
            const ids = searchResults.results.map(messageSearchResult => messageSearchResult.itemId);
            commit("setSortedIds", ids);
            return ServiceLocator.getProvider("MailboxItemsPersistance")
                .get(folderUid)
                .multipleById(ids)
                .then(items => {
                    commit("setItems", items);
                    commit("setCount", searchResults.totalResults);
                    commit("setSearchPattern", pattern);
                    commit("setSearchLoading", false);
                    commit("setSearchError", false);
                });
        })
        .catch(() => {
            commit("setItems", []);
            commit("setCount", 0);
            commit("setSearchPattern", pattern);
            commit("setSearchLoading", false);
            commit("setSearchError", true);
        });
}

export function remove(payload, { folderId, trashFolderId, mailId }) {
    return ServiceLocator.getProvider("MailboxFoldersPersistance")
        .get()
        .importItems(trashFolderId, {
            mailboxFolderId: folderId,
            ids: [{ id: mailId }],
            expectedIds: undefined,
            deleteFromSource: true
        });
}

/**
 * Wait until the draft is not saving or a timeout is reached.
 * @param {*} draft the draft message
 * @param {Number} delayTime the initial delay time between two checks
 * @param {Number} maxTries the maximum number of checks
 * @param {Number} iteration DO NOT SET. Only used internally for recursivity
 */
function waitUntilDraftNotSaving(draft, delayTime, maxTries, iteration = 1) {
    if (draft.status == DraftStatus.SAVING) {
        return new Promise(resolve => setTimeout(() => resolve(draft.status), delayTime))
            .then(status => {
                if (status != DraftStatus.SAVING) {
                    return Promise.resolve();
                } else {
                    if (iteration < maxTries) {
                        // 'smart' delay: add 250ms each retry
                        return waitUntilDraftNotSaving(draft, delayTime + 250, maxTries, ++iteration);
                    } else {
                        return Promise.reject("Timeout while waiting for the draft to be saved");
                    }
                }
            });
    } else {
        return Promise.resolve();
    }
}

/** Save the current draft: create it into Drafts box, delete the previous one. */
export function saveDraft({ commit, rootState, state }) {
    const previousDraftId = state.draft.id;
    let service;
    let draft;
    let userSession;

    // only one saveDraft at a time
    return waitUntilDraftNotSaving(state.draft, 250, 5)
        .then(() => {
            // prepare the message content and initialize services
            commit("updateDraft", { status: DraftStatus.SAVING });

            // deep clone
            draft = JSON.parse(JSON.stringify(state.draft));
            
            const previousMessage = draft.previousMessage;
            const isAReply = !!previousMessage;
            
            delete draft.id;
            delete draft.status;
            delete draft.saveDate;
            
            if (previousMessage && previousMessage.content
                && !draft.content.includes(previousMessage.content)) {
                draft.content += "\n\n\n" + previousMessage.content;
            }
            
            if (isAReply) {
                if (previousMessage.messageId) {
                    draft.headers.push({ name: "In-Reply-To", values: [previousMessage.messageId] });
                    draft.references = [previousMessage.messageId].concat(previousMessage.references);
                } else {
                    draft.references = previousMessage.references;
                }
            }
            
            sanitize(draft);
            
            const draftbox = rootState["backend.mail/folders"].folders.find(function (folder) {
                return folder.displayName === "Drafts";
            });
            
            service = ServiceLocator.getProvider("MailboxItemsPersistance").get(draftbox.uid);
            userSession = injector.getProvider("UserSession").get();
        })
        .then(() => {
            let partsToUpload = {};
            // FIXME cyrus wants \r\n, should do the replacement in the core, yes ?
            let content = draft.content.replace(/\r?\n/g, "\r\n");
            
            if (draft.type === "text") {
                partsToUpload[MimeType.TEXT_PLAIN] = content;
            } else if (draft.type === "html") {
                partsToUpload[MimeType.TEXT_HTML] = content;
                partsToUpload[MimeType.TEXT_PLAIN] = html2text(content).replace(/\r?\n/g, "\r\n");
            }
            
            // upload draft parts
            let addrParts = {};
            let promises = Object.entries(partsToUpload).map(uploadMe => 
                service.uploadPart(uploadMe[1]).then((addrPart) => { 
                    addrParts[uploadMe[0]] = addrPart; 
                    return Promise.resolve();
                })
            );
            return Promise.all(promises).then(() => addrParts);
        })
        .then(addrParts => {
            // create the new draft in the draftbox
            let structure;
            let textPart = {
                mime: MimeType.TEXT_PLAIN,
                address: addrParts[MimeType.TEXT_PLAIN]
            };

            if (draft.type === "text") {
                structure = textPart;
            } else if (draft.type === "html") {
                structure = {
                    mime: MimeType.MULTIPART_ALTERNATIVE,
                    children: [
                        textPart,
                        {
                            mime: MimeType.TEXT_HTML,
                            address: addrParts[MimeType.TEXT_HTML]
                        }
                    ]
                };
            }
            
            return service.create(
                new Message(draft)
                    .toMailboxItem(userSession.defaultEmail, userSession.formatedName, true, structure)
            );
        })
        .then(itemIdentifier => {
            // update the draft properties
            draft.id = itemIdentifier.id;
            commit("updateDraft", { status: DraftStatus.SAVED, saveDate: new Date(), id: itemIdentifier.id });
            if (previousDraftId) {
                // delete the previous draft
                return service.deleteById(previousDraftId);
            }
            return Promise.resolve();
        })
        .then(() => {
            return draft.id;
        })
        .catch(reason => {
            const key = "mail.alert.draft.save.error";
            const error = new Alert({
                code: "ALERT_CODE_MSG_DRAFT_SAVE_ERROR",
                key,
                message: getLocalizedProperty(userSession, key, { subject: draft.subject, reason }),
                props: {
                    subject: draft.subject,
                    reason
                }
            });
            commit("alert/addError", error, { root: true });
            commit("updateDraft", { status: DraftStatus.SAVE_ERROR, saveDate: null });
        });
}

/** Delete the draft (hard delete, not moved in Trash box). */
export function deleteDraft({ commit, rootState, state }) {
    const draft = state.draft;

    if (!draft.id || draft.status == DraftStatus.DELETED) {
        // no saved draft to delete, just close the composer
        return Promise.resolve();
    }

    let service;
    let userSession;
    return new Promise(resolve => {
        // initialize service, session and status
        const draftbox = rootState["backend.mail/folders"].folders.find(function (folder) {
            return folder.displayName === "Drafts";
        });
        service = ServiceLocator.getProvider("MailboxItemsPersistance").get(draftbox.uid);
        userSession = injector.getProvider("UserSession").get();
        commit("updateDraft", { status: DraftStatus.DELETING });
        return resolve();
    })
        .then(() => {
            // request a delete on core side
            return service.deleteById(draft.id);
        })
        .then(() => {
            const key = "mail.alert.message.draft.delete.ok";
            const success = new Alert({
                type: AlertTypes.SUCCESS,
                code: "ALERT_CODE_MSG_DRAFT_DELETE_OK",
                key,
                message: getLocalizedProperty(userSession, key, { subject: draft.subject }),
                props: { subject: draft.subject }
            });
            commit("alert/addSuccess", success, { root: true });
            commit("updateDraft", { status: DraftStatus.DELETED });
        })
        .catch(reason => {
            const key = "mail.alert.message.draft.delete.error";
            const error = new Alert({
                code: "ALERT_CODE_MSG_DRAFT_DELETE_ERROR",
                key,
                message: getLocalizedProperty(userSession, key, { subject: draft.subject, reason: reason }),
                props: {
                    subject: draft.subject,
                    reason
                }
            });
            commit("alert/addError", error, { root: true });
            commit("updateDraft", { status: DraftStatus.DELETE_ERROR });
        });
}

export function move({ commit, dispatch, rootGetters }, { item, messageId, messageSubject, index, newFolderPattern }) {
    const currentFolderId = rootGetters["backend.mail/folders/currentFolderId"];
    const service = ServiceLocator.getProvider("MailboxFoldersPersistance").get();
    let promise;
    let destinationFolderUid = item.uid;
    const uidMoveInProgress = uuid();
    const moveInProgress = new Alert({
        uid: uidMoveInProgress,
        code: "ALERT_CODE_MSG_MOVED_IN_PROGRESS",
        key: "mail.alert.move.in_progress",
        type: AlertTypes.LOADING,
        props: { subject: "mySubject" }
    });

    if (item.type && item.type === "create-folder") {
        item.name = item.name.replace(newFolderPattern, '');
        promise = dispatch("backend.mail/folders/create", item, { root: true })
            .then((itemIdentifier) => { 
                destinationFolderUid = itemIdentifier.uid;
                commit("alert/addAlert", 
                    new Alert({
                        type: AlertTypes.SUCCESS,
                        code: "ALERT_CODE_CREATE_FOLDER_OK",
                        key: "mail.alert.create_folder.ok",
                        props: { folderName: item.name }
                    }), 
                    { root: true }
                );
                commit("alert/addAlert", moveInProgress, { root: true });
                service.importItems(itemIdentifier.id, {
                    mailboxFolderId: currentFolderId,
                    ids: [{ id: messageId }],
                    expectedIds: undefined,
                    deleteFromSource: true
                });
            });
    } else {
        commit("alert/addAlert", moveInProgress, { root: true });
        promise = service.importItems(item.id, {
            mailboxFolderId: currentFolderId,
            ids: [{ id: messageId }],
            expectedIds: undefined,
            deleteFromSource: true
        });
    }

    promise.then(() => {
        commit("remove", index);
        commit("alert/addAlert", 
            new Alert({
                type: AlertTypes.SUCCESS,
                code: "ALERT_CODE_MSG_MOVE_OK",
                key: "mail.alert.move.ok",
                props: { 
                    subject: messageSubject,
                    folder: item,
                    folderNameLink: '/mail/' + destinationFolderUid + '/'
                }
            }), 
            { root: true }
        );
    }).catch(error => 
        commit("alert/addAlert", 
            new Alert({
                type: AlertTypes.ERROR,
                code: "ALERT_CODE_MSG_MOVE_ERROR",
                key: "mail.alert.move.error",
                props: { 
                    subject: messageSubject,
                    folderName: item.name,
                    reason: error.message
                }
            }), 
            { root: true })
    ).finally(() => commit("alert/removeAlert", uidMoveInProgress, { root: true }));
    
    return promise;
}