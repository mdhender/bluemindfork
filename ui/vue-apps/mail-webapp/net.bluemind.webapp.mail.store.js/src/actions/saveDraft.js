//FIXME: Refactor this

import { html2text } from "@bluemind/html-utils";
import { Message, DraftStatus } from "@bluemind/backend.mail.store";
import { MimeType } from "@bluemind/email";
import injector from "@bluemind/inject";

/** Save the current draft: create it into Drafts box, delete the previous one. */
export function saveDraft({ commit, state, getters }) {
    const service = injector.getProvider("MailboxItemsPersistence").get(getters.my.DRAFTS.uid);
    const userSession = injector.getProvider("UserSession").get();
    let previousDraftId, draft;

    // only one saveDraft at a time
    return waitUntilDraftNotSaving(getters, 250, 5)
        .then(() => {
            commit("draft/update", { status: DraftStatus.SAVING });

            previousDraftId = state.draft.id;
            draft = JSON.parse(JSON.stringify(state.draft));

            delete draft.id;
            delete draft.status;
            delete draft.saveDate;

            handleReplyOrForward(draft);
            sanitize(draft);

            return uploadInlineParts(draft, service);
        })
        .then(addrParts => createDraftStructure(draft, addrParts, service, userSession, state))
        .then(({ id }) => {
            commit("draft/update", { status: DraftStatus.SAVED, saveDate: new Date(), id });
            if (previousDraftId) {
                return service.deleteById(previousDraftId).then(() => id);
            }
            return id;
        })
        .catch(() => {
            commit("draft/update", { status: DraftStatus.SAVE_ERROR, saveDate: null });
        });
}

function createDraftStructure(draft, addrParts, service, userSession, state) {
    let structure;
    const textPart = createTextPart(addrParts);
    if (draft.type === "text") {
        structure = textPart;
    } else if (draft.type === "html") {
        const htmlPart = createHtmlPart(addrParts);
        structure = {
            mime: MimeType.MULTIPART_ALTERNATIVE,
            children: [textPart, htmlPart]
        };
    }

    structure = handleAttachments(state, structure);

    return service.create(
        new Message(null, draft).toMailboxItem(userSession.defaultEmail, userSession.formatedName, true, structure)
    );
}

function handleAttachments(state, structure) {
    if (state.draft.parts.attachments.length > 0) {
        let children = [structure];
        const attachments = state.draft.parts.attachments.filter(
            a => state.draft.attachmentStatuses[a.uid] !== "ERROR"
        );
        children.push(...attachments);
        structure = {
            mime: MimeType.MULTIPART_MIXED,
            children
        };
    }
    return structure;
}

function createHtmlPart(addrParts) {
    return {
        mime: MimeType.TEXT_HTML,
        address: addrParts[MimeType.TEXT_HTML],
        encoding: "quoted-printable",
        charset: "utf-8"
    };
}

function createTextPart(addrParts) {
    return {
        mime: MimeType.TEXT_PLAIN,
        address: addrParts[MimeType.TEXT_PLAIN],
        encoding: "quoted-printable",
        charset: "utf-8"
    };
}

function uploadInlineParts(draft, service) {
    let partsToUpload = {};

    // FIXME cyrus wants \r\n, should do the replacement in the core, yes ?
    let content = draft.content.replace(/\r?\n/g, "\r\n");

    if (draft.type === "text") {
        partsToUpload[MimeType.TEXT_PLAIN] = content;
    } else if (draft.type === "html") {
        partsToUpload[MimeType.TEXT_HTML] = content;
        partsToUpload[MimeType.TEXT_PLAIN] = html2text(content).replace(/\r?\n/g, "\r\n");
    }

    let addrParts = {};
    let promises = Object.entries(partsToUpload).map(uploadMe =>
        service.uploadPart(uploadMe[1]).then(addrPart => {
            addrParts[uploadMe[0]] = addrPart;
            return Promise.resolve();
        })
    );
    return Promise.all(promises).then(() => addrParts);
}

function handleReplyOrForward(draft) {
    const previousMessage = draft.previousMessage;
    const isAReplyOrForward = !!previousMessage;

    if (isAReplyOrForward) {
        if (previousMessage.content && !draft.isReplyExpanded) {
            draft.content += previousMessage.content;
        }

        if (previousMessage.messageId) {
            draft.headers.push({ name: "In-Reply-To", values: [previousMessage.messageId] });
            draft.references = [previousMessage.messageId].concat(previousMessage.references);
        } else {
            draft.references = previousMessage.references;
        }
    }
}

function sanitize(messageToSend) {
    if (messageToSend.subject === "") {
        messageToSend.subject = "(No subject)";
    }
    if (messageToSend.content) {
        messageToSend.content = messageToSend.content.replace(/[\n\r]/g, String.fromCharCode(13, 10));
    }
}

/**
 * Wait until the draft is not saving or a timeout is reached.
 * @param {*} draft the draft message
 * @param {Number} delayTime the initial delay time between two checks
 * @param {Number} maxTries the maximum number of checks
 * @param {Number} iteration DO NOT SET. Only used internally for recursivity
 */
function waitUntilDraftNotSaving(getters, delayTime, maxTries, iteration = 1) {
    const draftStatus = getters["draft/status"];
    if (draftStatus === DraftStatus.SAVING) {
        return new Promise(resolve => setTimeout(() => resolve(draftStatus), delayTime)).then(status => {
            if (status !== DraftStatus.SAVING) {
                return Promise.resolve();
            } else {
                if (iteration < maxTries) {
                    // 'smart' delay: add 250ms each retry
                    return waitUntilDraftNotSaving(getters, delayTime + 250, maxTries, ++iteration);
                } else {
                    return Promise.reject("Timeout while waiting for the draft to be saved");
                }
            }
        });
    } else {
        return Promise.resolve();
    }
}
