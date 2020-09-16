import { html2text } from "@bluemind/html-utils";
import { MimeType, PartsHelper } from "@bluemind/email";
import DraftStatus from "../mailbackend/MailboxItemsStore/DraftStatus";
import injector from "@bluemind/inject";
import MessageAdaptor from "../../store/messages/MessageAdaptor";
import mutationTypes from "../../store/mutationTypes";

/** Save the current draft: create it into Drafts box, delete the previous one. */
export function saveDraft({ commit, getters, rootGetters }, { message, editorContent, userPrefTextOnly }) {
    const service = injector.getProvider("MailboxItemsPersistence").get(rootGetters["mail/MY_DRAFTS"].key);
    const userSession = injector.getProvider("UserSession").get();
    let draft = { ...message };

    // only one saveDraft at a time
    return waitUntilDraftNotSaving(getters, 250, 5)
        .then(() => {
            commit("mail/" + mutationTypes.SET_DRAFT_STATUS, DraftStatus.SAVING, { root: true });
            draft = prepareDraft(draft, editorContent, userPrefTextOnly);
            return uploadInlineParts(draft, service);
        })
        .then(addrParts => {
            draft.addrParts = addrParts;
            return createDraftStructure(draft, service, userSession);
        })
        .then(({ id }) => {
            draft.remoteRef.internalId = id;
            return cleanParts(draft, service);
        })
        .then(() => {
            // FIXME set DraftStatus in a "composer" state because we need it in toolbar + composer
            commit("mail/" + mutationTypes.SET_DRAFT_SAVE_DATE, new Date(), { root: true });
            commit("mail/" + mutationTypes.SET_DRAFT_STATUS, DraftStatus.SAVED, { root: true });
            if (draft.previousId) {
                return service.deleteById(draft.previousId).then(() => draft.remoteRef.internalId);
            }
            return draft.remoteRef.internalId;
        })
        .catch(() => {
            commit("mail/" + mutationTypes.SET_DRAFT_STATUS, DraftStatus.SAVE_ERROR, { root: true });
        });
}

function createDraftStructure(draft, service, userSession, userPrefTextOnly) {
    let structure;
    const textPart = PartsHelper.createTextPart(draft.addrParts[MimeType.TEXT_PLAIN][0]);

    if (userPrefTextOnly) {
        structure = textPart;
    } else {
        const htmlPart = PartsHelper.createHtmlPart(draft.addrParts[MimeType.TEXT_HTML][0]);
        structure = PartsHelper.createAlternativePart(textPart, htmlPart);
        structure = PartsHelper.createInlineImageParts(structure, draft.addrParts[MimeType.IMAGE], draft.inlineImages);
    }
    structure = PartsHelper.createAttachmentParts(
        draft.attachments,
        draft.attachmentStatuses, // FIXME
        structure
    );
    return service.create(
        MessageAdaptor.realToMailboxItem(draft, userSession.defaultEmail, userSession.formatedName, true, structure)
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

function prepareDraft(draft, editorContent, userPrefTextOnly) {
    draft = Object.assign(draft, { previousId: draft.remoteRef.internalId, id: undefined });

    handleReplyOrForward(draft);
    sanitize(draft, editorContent);

    draft.partsToUpload = {};
    if (userPrefTextOnly) {
        draft.inlineImages = [];
        draft.partsToUpload[MimeType.TEXT_PLAIN] = [editorContent];
    } else {
        const cidResults = PartsHelper.insertCid(editorContent);
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

function sanitize(messageToSend, editorContent) {
    if (messageToSend.subject === "") {
        messageToSend.subject = "(No subject)";
    }
    if (editorContent) {
        editorContent = editorContent.replace(/[\n\r]/g, String.fromCharCode(13, 10));
    }
    // FIXME cyrus wants \r\n, should do the replacement in the core, yes ?
    editorContent = editorContent.replace(/\r?\n/g, "\r\n");
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
