import { EmailValidator, MimeType } from "@bluemind/email";
import injector from "@bluemind/inject";
import PartsHelper from "./PartsHelper";
import ServiceLocator from "@bluemind/inject";
import uuid from "uuid/v4";
import Message from "./Message.js";
import { getLocalizedProperty } from "@bluemind/backend.mail.l10n";
import { ItemFlag } from "@bluemind/core.container.api";

export function $_mailrecord_changed({ dispatch }, { container }) {
    return dispatch("all", container);
}

export function all({ commit }, folder) {
    const service = ServiceLocator.getProvider("MailboxItemsPersistance").get(folder);
    return service
        .filteredChangesetById(0, { must: [], mustNot: [ ItemFlag.Deleted ] })
        .then(result => {
            let ids = result.created.map(itemVersion => itemVersion.id);
            if (ids.length >= 500) {
                ids = ids.slice(0, 499);
            }
            return service.multipleById(ids);
        })
        .then(items => {
            commit("setItems", items);
            commit("setCount", items.length);
        });
}

/* TODO remove when webapp store is ready : following calls should be done in webapp, not in mailbackend */
export function select({ state, commit, getters, dispatch }, { folder, uid }) {
    // 1) Select the current message to display
    commit("setCurrent", uid);

    // 2) Obtain the different display possibilities
    const result = visitParts(getters, uid);
    
    // 3) Choose parts to display
    let chosenParts = choosePartsToDisplay(result.inlines);
    
    // 4) Retrieve parts content
    return fetchParts(state, commit, folder, uid, dispatch, chosenParts).then(() => {
        // 5) Manipulate parts content before display
        chosenParts = processBeforeDisplay(state, uid, chosenParts);

        // 6) Add attachments
        return setAttachments(commit, dispatch, folder, uid, result.attachments).then(() =>
            // 7) Render the parts
            displayParts(commit, uid, chosenParts)
        );
    });
}

/**
 * Obtain the different display possibilities.
 *
 * TODO move to webapp store or router when ready.
 */
function visitParts(getters, uid) {
    const message = getters.messageByUid(uid);
    return message.computeParts();
}

function setAttachments(commit, dispatch, folder, uid, attachments) {
    let promises = attachments
        .filter(a => MimeType.previewAvailable(a.mime))
        .map(part => dispatch("fetch", { folder, uid, part, isAttachment: true }));

    return Promise.all(promises).then(commit("setAttachments", attachments));
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
function fetchParts(state, commit, folder, uid, dispatch, partsToFetch) {
    // use mailbackend to fetch parts
    let promises = partsToFetch.map(part => dispatch("fetch", { folder, uid, part, isAttachment: false }));
    return Promise.all(promises).catch(reason => {
        commit("alert/add", { uid: uuid(), type: "danger", message: "Failed to read mail " + reason });
    });
}

/**
 * Manipulate parts content before display.
 *
 * TODO move to webapp store or router when ready.
 */
function processBeforeDisplay(state, uid, fetchedParts) {
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
function displayParts(commit, uid, partsToDisplay) {
    commit("setPartsToDisplay", partsToDisplay);
}

export function fetch({ state }, { folder, uid, part, isAttachment }) {
    const item = state.items.find(item => item.uid == uid);
    let encoding = part.encoding;
    if (isAttachment || MimeType.isImage(part)) {
        encoding = null;
    }
    return ServiceLocator.getProvider("MailboxItemsPersistance")
        .get(folder)
        .fetch(item.value.imapUid, part.address, encoding, part.mime, part.charset)
        .then(function (stream) {
            part.content = stream;
            part.uid = uid;
            part.cid = part.contentId;
        });
}

export function updateSeen({ state, commit }, { folder, uid, isSeen }) {
    const itemId = state.items.find(item => item.uid === uid).internalId;
    return ServiceLocator.getProvider("MailboxItemsPersistance")
        .get(folder)
        .updateSeens([{ itemId: itemId, seen: isSeen, mdnSent: false }])
        .then(() => commit("updateSeen", { uid: uid, isSeen: isSeen }));
}

export function send(payload, { message, isAReply, previousMessage, outboxUid }) {
    if (isAReply) {
        if (previousMessage.messageId) {
            message.headers.push({ name: "In-Reply-To", values: [previousMessage.messageId] });
            message.references = [previousMessage.messageId].concat(previousMessage.references);
        } else {
            message.references = previousMessage.references;
        }
    }

    const userSession = injector.getProvider("UserSession").get();

    if (!validate(message)) {
        return Promise.reject(getLocalizedProperty(userSession, "mail.error.email.address.invalid"));
    }

    sanitize(message);

    const service = ServiceLocator.getProvider("MailboxItemsPersistance").get(outboxUid);
    const outboxService = ServiceLocator.getProvider("OutboxPersistance").get();

    return service
        .uploadPart(message.content)
        .then(addrPart =>
            service.create(
                new Message(message).toMailboxItem(addrPart, userSession.defaultEmail, userSession.formatedName, true)
            )
        )
        .then(() => outboxService.flush()); // TODO: this request returns a taskref ID, we have to track taskref state)
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
    return ServiceLocator.getProvider("MailboxFoldersPersistance").get().searchItems({
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
    }).then(searchResults => {
        const ids = searchResults.results.map(messageSearchResult => messageSearchResult.itemId);
        return ServiceLocator.getProvider("MailboxItemsPersistance").get(folderUid).multipleById(ids).then(items => {
            commit("setItems", items);
            commit("setCount", searchResults.totalResults);
            commit("setSearchPattern", pattern);
            commit("setSearchLoading", false);
            commit("setSearchError", false);
        });
    }).catch(() => {
        commit("setItems", []);
        commit("setCount", 0);
        commit("setSearchPattern", pattern);
        commit("setSearchLoading", false);
        commit("setSearchError", true);
    })
    ;
}