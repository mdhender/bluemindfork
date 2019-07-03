import { EmailValidator, MimeType } from "@bluemind/email";
import injector from "@bluemind/inject";
import PartsHelper from "./PartsHelper";
import ServiceLocator from "@bluemind/inject";
import uuid from "uuid/v4";

export function all({ commit }, folder) {
    const service = ServiceLocator.getProvider("MailboxItemsPersistance").get(folder);
    return service
        .sortedIds({ column: "internal_date", direction: "desc" })
        .then(uids => service.multipleById(uids))
        .then(items => {
            commit("setItems", items);
            commit("setCount", items.length);
        });
}

/** TODO remove when webapp store is ready */
export function select({ state, commit, getters, dispatch }, { folder, uid }) {
    // FIXME following calls should be done in webapp, not in mailbackend

    // 1) Select the current message to display
    commit("setCurrent", uid);

    // 2) Add attachments FIXME
    commit("setAttachments", ["application.log 1,8Mo", "flyer.odt 450Ko"]);

    // 3) Obtain the different display possibilities
    const partsByCapabilities = retrievePartsByCapabilities(getters, uid);

    // 4) Chose what to display
    let chosenParts = chosePartsToDisplay(partsByCapabilities);

    // 5) Retrieve parts content
    return fetchParts(state, commit, folder, uid, dispatch, chosenParts).then(() => {
        // 6) Manipulate parts content before display
        chosenParts = processBeforeDisplay(state, uid, chosenParts);
        // 7) Render the parts
        displayParts(commit, uid, chosenParts);
    });
}

/**
 * Obtain the different display possibilities.
 *
 * TODO move to webapp store or router when ready.
 */
function retrievePartsByCapabilities(getters, uid) {
    const message = getters.messageByUid(uid);
    return message.getInlineParts(message.structure);
}

/**
 * Chose what to display.
 *
 * TODO move to webapp store or router when ready.
 */
function chosePartsToDisplay(partsByCapabilities) {
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
    let promises = partsToFetch.map(part => dispatch("fetch", { folder, uid, part }));
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

export function fetch({ state }, { folder, uid, part }) {
    const item = state.items.find(item => item.uid == uid);
    let encoding = part.encoding;
    if (MimeType.isImage(part)) {
        encoding = null;
    }
    return ServiceLocator.getProvider("MailboxItemsPersistance")
        .get(folder)
        .fetch(item.value.imapUid, part.address, encoding)
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
        .updateSeens([{ itemId: itemId, seen: isSeen, mdnSent: true }])
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
    if (!validate(message)) {
        return;
    }
    sanitize(message);
    const userSession = injector.getProvider('UserSession').get();
    const service = ServiceLocator.getProvider("MailboxItemsPersistance").get(outboxUid);
    const outboxService = ServiceLocator.getProvider("OutboxPersistance").get();

    return service
        .uploadPart(message.content)
        .then(addrPart => service.create(
            message.toMailboxItem(
                addrPart,
                userSession.defaultEmail,
                userSession.domain
            )))
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