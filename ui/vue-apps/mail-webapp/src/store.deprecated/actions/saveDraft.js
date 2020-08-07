import { html2text } from "@bluemind/html-utils";
import { MimeType, PartsHelper } from "@bluemind/email";
import DraftStatus from "../mailbackend/MailboxItemsStore/DraftStatus";
import injector from "@bluemind/inject";
import Message from "../mailbackend/MailboxItemsStore/Message";

/** Save the current draft: create it into Drafts box, delete the previous one. */
export function saveDraft({ commit, state, getters, rootGetters }) {
    const service = injector.getProvider("MailboxItemsPersistence").get(rootGetters["mail/MY_DRAFTS"].key);
    const userSession = injector.getProvider("UserSession").get();
    let draft;

    // only one saveDraft at a time
    return waitUntilDraftNotSaving(getters, 250, 5)
        .then(() => {
            commit("draft/update", { status: DraftStatus.SAVING });
            draft = prepareDraft(draft, state);
            return uploadInlineParts(draft, service);
        })
        .then(addrParts => {
            draft.addrParts = addrParts;
            return createDraftStructure(draft, service, userSession, state);
        })
        .then(({ id }) => {
            draft.id = id;
            return cleanParts(draft, service);
        })
        .then(() => {
            commit("draft/update", { status: DraftStatus.SAVED, saveDate: new Date(), id: draft.id });
            if (draft.previousId) {
                return service.deleteById(draft.previousId).then(() => draft.id);
            }
            return draft.id;
        })
        .catch(() => {
            commit("draft/update", { status: DraftStatus.SAVE_ERROR, saveDate: null });
        });
}

function createDraftStructure(draft, service, userSession, state) {
    let structure;
    const textPart = PartsHelper.createTextPart(draft.addrParts[MimeType.TEXT_PLAIN][0]);

    if (draft.type === "text") {
        structure = textPart;
    } else if (draft.type === "html") {
        const htmlPart = PartsHelper.createHtmlPart(draft.addrParts[MimeType.TEXT_HTML][0]);
        structure = PartsHelper.createAlternativePart(textPart, htmlPart);
        structure = PartsHelper.createInlineImageParts(structure, draft.addrParts[MimeType.IMAGE], draft.inlineImages);
    }
    structure = PartsHelper.createAttachmentParts(
        state.draft.parts.attachments,
        state.draft.attachmentStatuses,
        structure
    );
    return service.create(
        new Message(null, draft).toMailboxItem(userSession.defaultEmail, userSession.formatedName, true, structure)
    );
}

function cleanParts(draft, service) {
    const promises = [];
    Object.keys(draft.addrParts).forEach(mimeType => {
        draft.addrParts[mimeType].forEach(address => {
            promises.push(service.removePart(address));
        });
    });
    return Promise.all(promises);
}

function prepareDraft(draft, state) {
    draft = JSON.parse(JSON.stringify(state.draft));
    draft = Object.assign(draft, { previousId: draft.id, id: undefined });

    handleReplyOrForward(draft);
    sanitize(draft);
    // FIXME cyrus wants \r\n, should do the replacement in the core, yes ?
    draft.content = draft.content.replace(/\r?\n/g, "\r\n");

    draft.partsToUpload = {};
    if (draft.type === "text") {
        draft.inlineImages = [];
        draft.partsToUpload[MimeType.TEXT_PLAIN] = [draft.content];
    } else if (draft.type === "html") {
        const cidResults = PartsHelper.insertCid(draft.content);
        const html = cidResults.html;
        draft.inlineImages = cidResults.images;
        draft.partsToUpload[MimeType.TEXT_HTML] = [html];
        draft.partsToUpload[MimeType.TEXT_PLAIN] = [html2text(html).replace(/\r?\n/g, "\r\n")];
        draft.partsToUpload[MimeType.IMAGE] = draft.inlineImages.map(image => image.content);
    }

    return draft;
}

function uploadInlineParts(draft, service) {
    const addrParts = {};
    let promises = [];

    Object.entries(draft.partsToUpload).forEach(partsToUploadByMimeType => {
        const mimeType = partsToUploadByMimeType[0];
        const parts = partsToUploadByMimeType[1];
        addrParts[mimeType] = [];
        const uploadedPromises = parts.map(part =>
            service.uploadPart(part).then(addrPart => {
                addrParts[mimeType].push(addrPart);
            })
        );
        promises.push(...uploadedPromises);
    });

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
