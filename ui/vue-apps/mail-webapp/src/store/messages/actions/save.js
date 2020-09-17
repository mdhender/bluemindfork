import { html2text } from "@bluemind/html-utils";
import { MimeType, PartsHelper } from "@bluemind/email";
import { inject } from "@bluemind/inject";

import MessageAdaptor from "../MessageAdaptor";
import MessageStatus from "../MessageStatus";
import mutationTypes from "../../mutationTypes";

/** Save the current draft: create it into Drafts box, delete the previous one. */
export default async function ({ commit, state }, { userPrefTextOnly, draftKey, myDraftsFolderKey, editorContent }) {
    try {
        const service = inject("MailboxItemsPersistence", myDraftsFolderKey);

        await waitUntilDraftNotSaving(state[draftKey], 250, 5); // only one saveDraft at a time
        commit(mutationTypes.SET_MESSAGES_STATUS, [{ key: draftKey, status: MessageStatus.SAVING }]);

        const draft = { ...state[draftKey] };
        const previousDraftId = draft.remoteRef.internalId;

        let { partsToUpload, inlineImages } = prepareDraft(draft, editorContent, userPrefTextOnly);
        const addrParts = await uploadInlineParts(service, partsToUpload);
        const newDraftId = await createDraftStructure(draft, service, userPrefTextOnly, addrParts, inlineImages);

        commit(mutationTypes.SET_MESSAGE_INTERNAL_ID, { messageKey: draft.key, internalId: newDraftId });
        commit(mutationTypes.SET_MESSAGE_DATE, { messageKey: draft.key, date: new Date() });
        commit(mutationTypes.SET_MESSAGES_STATUS, [{ key: draftKey, status: MessageStatus.LOADED }]);

        await cleanParts(service, addrParts);
        if (previousDraftId) {
            await service.deleteById(previousDraftId);
        }

        return newDraftId;
    } catch (e) {
        commit(mutationTypes.SET_MESSAGES_STATUS, [{ key: draftKey, status: MessageStatus.SAVE_ERROR }]);
        throw e;
    }
}

async function createDraftStructure(draft, service, userPrefTextOnly, addrParts, inlineImages) {
    let structure;
    const textPart = PartsHelper.createTextPart(addrParts[MimeType.TEXT_PLAIN][0]);

    if (userPrefTextOnly) {
        structure = textPart;
    } else {
        const htmlPart = PartsHelper.createHtmlPart(addrParts[MimeType.TEXT_HTML][0]);
        structure = PartsHelper.createAlternativePart(textPart, htmlPart);
        structure = PartsHelper.createInlineImageParts(structure, addrParts[MimeType.IMAGE], inlineImages);
    }
    structure = PartsHelper.createAttachmentParts(
        draft.attachments,
        draft.attachmentStatuses, // FIXME
        structure
    );
    const createResult = await service.create(MessageAdaptor.realToMailboxItem(draft, structure));
    return createResult.id;
}

function cleanParts(service, addrParts) {
    const promises = [];
    Object.keys(addrParts).forEach(mimeType => {
        addrParts[mimeType].forEach(address => {
            promises.push(service.removePart(address));
        });
    });
    return Promise.all(promises);
}

function prepareDraft(draft, editorContent, userPrefTextOnly) {
    const partsToUpload = {};
    let inlineImages = [];

    // handleReplyOrForward(draft); FIXME
    sanitize(draft, editorContent);

    if (userPrefTextOnly) {
        partsToUpload[MimeType.TEXT_PLAIN] = [editorContent];
    } else {
        const cidResults = PartsHelper.insertCid(editorContent);
        const html = cidResults.html;
        inlineImages = cidResults.images;
        partsToUpload[MimeType.TEXT_HTML] = [html];
        partsToUpload[MimeType.TEXT_PLAIN] = [html2text(html).replace(/\r?\n/g, "\r\n")];
        partsToUpload[MimeType.IMAGE] = inlineImages.map(image => image.content);
    }

    return { partsToUpload, inlineImages };
}

async function uploadInlineParts(service, partsToUpload) {
    const addrParts = {};
    let promises = [];

    Object.entries(partsToUpload).forEach(partsToUploadByMimeType => {
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

    await Promise.all(promises);
    return addrParts;
}

// FIXME
// function handleReplyOrForward(draft) {
//     const previousMessage = draft.previousMessage;
//     const isAReplyOrForward = !!previousMessage;

//     if (isAReplyOrForward) {
//         if (previousMessage.content && !draft.isReplyExpanded) {
//             draft.content += previousMessage.content;
//         }

//         if (previousMessage.messageId) {
//             draft.headers.push({ name: "In-Reply-To", values: [previousMessage.messageId] });
//             draft.references = [previousMessage.messageId].concat(previousMessage.references);
//         } else {
//             draft.references = previousMessage.references;
//         }
//     }
// }

function sanitize(draft, editorContent) {
    if (draft.subject === "") {
        draft.subject = "(No subject)";
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
function waitUntilDraftNotSaving(draft, delayTime, maxTries, iteration = 1) {
    if (draft.status === MessageStatus.SAVING) {
        return new Promise(resolve => setTimeout(() => resolve(draft.status), delayTime)).then(status => {
            if (status !== MessageStatus.SAVING) {
                return Promise.resolve();
            } else {
                if (iteration < maxTries) {
                    // 'smart' delay: add 250ms each retry
                    return waitUntilDraftNotSaving(draft.status, delayTime + 250, maxTries, ++iteration);
                } else {
                    return Promise.reject("Timeout while waiting for the draft to be saved");
                }
            }
        });
    } else {
        return Promise.resolve();
    }
}
