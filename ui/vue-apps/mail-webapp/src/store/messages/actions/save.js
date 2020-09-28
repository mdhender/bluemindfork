import { html2text, sanitizeHtml } from "@bluemind/html-utils";
import { MimeType, InlineImageHelper } from "@bluemind/email";
import { inject } from "@bluemind/inject";

import MessageAdaptor from "../helpers/MessageAdaptor";
import MessageBuilder from "../helpers/MessageBuilder";
import { MessageStatus, MessageHeader } from "../../../model/message";
import mutationTypes from "../../mutationTypes";
import PartsHelper from "../helpers/PartsHelper";

export default async function ({ commit, state }, { userPrefTextOnly, draftKey, myDraftsFolderKey, messageCompose }) {
    try {
        await waitUntilDraftNotSaving(state[draftKey], 250, 5); // only one saveDraft at a time

        commit(mutationTypes.SET_MESSAGES_STATUS, [{ key: draftKey, status: MessageStatus.SAVING }]);

        const draft = state[draftKey];
        const editorContent = prepareEditorContent(messageCompose);
        let { partsToUpload, inlineImages } = prepareDraft(draft, editorContent, userPrefTextOnly, commit);

        const service = inject("MailboxItemsPersistence", myDraftsFolderKey);
        const inlinePartAddresses = await uploadInlineParts(service, partsToUpload);

        const structure = MessageBuilder.createDraftStructure(
            draft,
            userPrefTextOnly,
            inlinePartAddresses,
            inlineImages
        );
        await service.updateById(draft.remoteRef.internalId, MessageAdaptor.realToMailboxItem(draft, structure));

        commit(mutationTypes.SET_MESSAGES_STATUS, [{ key: draftKey, status: MessageStatus.LOADED }]);

        PartsHelper.clean(
            inlinePartAddresses,
            draft.attachments.map(a => a.address).filter(address => address.length > 5),
            service
        );
    } catch (e) {
        commit(mutationTypes.SET_MESSAGES_STATUS, [{ key: draftKey, status: MessageStatus.SAVE_ERROR }]);
        throw e;
    }
}

function prepareDraft(draft, editorContent, userPrefTextOnly, commit) {
    forceMailRewriteOnServer(draft, commit);
    const partsToUpload = {};
    let inlineImages = [];

    if (userPrefTextOnly) {
        partsToUpload[MimeType.TEXT_PLAIN] = [editorContent];
    } else {
        const previousInlineImages = draft.inlinePartsByCapabilities
            .find(byCapabilities => byCapabilities.capabilities[0] === MimeType.TEXT_HTML)
            .parts.filter(part => part.dispositionType === "INLINE" && part.mime.startsWith(MimeType.IMAGE));
        const insertCidsResults = InlineImageHelper.insertCid(editorContent, previousInlineImages);
        inlineImages = insertCidsResults.inlineImages;
        const inlineImagesToUpload = inlineImages.filter(part => !part.address);

        const html = insertCidsResults.html;
        partsToUpload[MimeType.TEXT_HTML] = [html];
        partsToUpload[MimeType.TEXT_PLAIN] = [html2text(html).replace(/\r?\n/g, "\r\n")];
        partsToUpload[MimeType.IMAGE] = inlineImagesToUpload.map(part => insertCidsResults.streamByCid[part.contentId]);
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

/**
 * Needed by BM core to detect if mail has changed when using IMailboxItems.updateById
 */
function forceMailRewriteOnServer(draft, commit) {
    const headers = JSON.parse(JSON.stringify(draft.headers));
    const saveDate = new Date();
    commit(mutationTypes.SET_MESSAGE_DATE, { messageKey: draft.key, date: saveDate });

    const hasXBmDraftKeyHeader = headers.find(header => header.name === MessageHeader.X_BM_DRAFT_REFRESH_DATE);
    if (hasXBmDraftKeyHeader) {
        hasXBmDraftKeyHeader.values = [saveDate.getTime()];
    } else {
        headers.push({
            name: MessageHeader.X_BM_DRAFT_REFRESH_DATE,
            values: [saveDate.getTime()]
        });
    }
    commit(mutationTypes.SET_MESSAGE_HEADERS, { messageKey: draft.key, headers });
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

function prepareEditorContent(messageCompose) {
    let editorContent = messageCompose.collapsedContent
        ? messageCompose.editorContent + messageCompose.collapsedContent
        : messageCompose.editorContent;
    editorContent = sanitizeHtml(editorContent);
    editorContent = MessageBuilder.sanitizeForCyrus(editorContent);
    return editorContent;
}
