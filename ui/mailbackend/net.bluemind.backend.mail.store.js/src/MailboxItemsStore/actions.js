import { EmailValidator, MimeType } from "@bluemind/email";
import injector from "@bluemind/inject";
import PartsHelper from "./PartsHelper";
import ServiceLocator from "@bluemind/inject";
import uuid from "uuid/v4";
import Message from "./Message.js";
import { getLocalizedProperty } from "@bluemind/backend.mail.l10n";
import { ItemFlag } from "@bluemind/core.container.api";
import { AlertTypes, Alert } from "@bluemind/alert.store";

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
        commit("alert/add", { uid: uuid(), type: "danger", message: "Failed to read mail " + reason });
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
        .then(function(stream) {
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

export function send({ state, rootState, commit }) {
    const draftMail = state.draftMail;
    const previousMessage = draftMail.previousMessage;
    const isAReply = !!previousMessage;

    const messageToSend = JSON.parse(JSON.stringify(draftMail));
    if (previousMessage && previousMessage.content && !messageToSend.content.includes(previousMessage.content)) {
        messageToSend.content += "\n\n\n" + previousMessage.content;
    }

    const outboxUid = rootState["backend.mail/folders"].folders.find(function(folder) {
        return folder.displayName === "Outbox";
    }).uid;

    const sentboxUid = rootState["backend.mail/folders"].folders.find(function(folder) {
        return folder.displayName === "Sent";
    }).uid;

    if (isAReply) {
        if (previousMessage.messageId) {
            messageToSend.headers.push({ name: "In-Reply-To", values: [previousMessage.messageId] });
            messageToSend.references = [previousMessage.messageId].concat(previousMessage.references);
        } else {
            messageToSend.references = previousMessage.references;
        }
    }

    const userSession = injector.getProvider("UserSession").get();

    if (!validate(messageToSend)) {
        const key = "mail.error.email.address.invalid";
        const error = new Alert({
            code: "ALERT_CODE_MSG_ADDRESS_INVALID",
            key,
            message: getLocalizedProperty(userSession, key)
        });
        commit("alert/addError", error, { root: true });
        return Promise.resolve();
    }

    sanitize(messageToSend);

    const service = ServiceLocator.getProvider("MailboxItemsPersistance").get(outboxUid);

    let mailId;
    return service
        .uploadPart(messageToSend.content)
        .then(addrPart =>
            service.create(
                new Message(messageToSend).toMailboxItem(
                    addrPart,
                    userSession.defaultEmail,
                    userSession.formatedName,
                    true
                )
            )
        )
        .then(itemIdentifier => {
            mailId = itemIdentifier.id;
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
                let importedMailboxItem = taskResult.result.find(r => r.source == mailId);
                const sentboxItemsService = ServiceLocator.getProvider("MailboxItemsPersistance").get(sentboxUid);
                return sentboxItemsService.getCompleteById(importedMailboxItem.destination);
            } else {
                throw "Unable to retrieve task result";
            }
        })
        .then(mailItem => {
            const mailImapUid = mailItem.value.imapUid;
            const key = "mail.alert.message.sent.ok";
            const success = new Alert({
                type: AlertTypes.SUCCESS,
                code: "ALERT_CODE_MSG_SENT_OK",
                key,
                message: getLocalizedProperty(userSession, key, { subject: messageToSend.subject }),
                props: {
                    subject: messageToSend.subject,
                    subjectLink: "/mail/" + sentboxUid + "/" + mailImapUid + "."
                }
            });
            commit("alert/addSuccess", success, { root: true });
            commit("setDraftMail", null);
        })
        .catch(reason => {
            const key = "mail.alert.message.sent.error";
            const error = new Alert({
                code: "ALERT_CODE_MSG_SENT_ERROR",
                key,
                message: getLocalizedProperty(userSession, key, { subject: messageToSend.subject, reason: reason }),
                props: {
                    subject: messageToSend.subject,
                    reason
                }
            });
            commit("alert/addError", error, { root: true });
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

export function move({ commit, dispatch, rootGetters }, { item, mailId, index, newFolderPattern }) {
    const currentFolderId = rootGetters["backend.mail/folders/currentFolderId"];
    const service = ServiceLocator.getProvider("MailboxFoldersPersistance").get();
    let promise = undefined;

    if (item.type && item.type === "create-folder") {
        item.name = item.name.replace(newFolderPattern, '');
        promise = dispatch("backend.mail/folders/create", item, { root: true })
            .then((itemIdentifier) => service.importItems(itemIdentifier.id, {
                mailboxFolderId: currentFolderId,
                ids: [{ id: mailId }],
                expectedIds: undefined,
                deleteFromSource: true
            }));
    } else {
        promise = service.importItems(item.id, {
            mailboxFolderId: currentFolderId,
            ids: [{ id: mailId }],
            expectedIds: undefined,
            deleteFromSource: true
        });
    }

    commit("remove", index);

    promise.then(() => {
        console.log("message moved successfully"); // REMOVE ME when alerts are set
    }).catch(() => 
        console.error("failed moving the message...") // REMOVE ME when alerts are set
    );
    return promise;
}
