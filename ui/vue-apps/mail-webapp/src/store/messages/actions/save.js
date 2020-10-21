import { inject } from "@bluemind/inject";

import apiMessages from "../../api/apiMessages";
import MessageAdaptor from "../helpers/MessageAdaptor";
import { MessageStatus, clean } from "../../../model/message";
import { createDraftStructure, forceMailRewriteOnServer, prepareDraft } from "../../../model/draft";
import mutationTypes from "../../mutationTypes";

export default async function ({ commit, state }, { userPrefTextOnly, draftKey, myDraftsFolderKey, messageCompose }) {
    try {
        await waitUntilDraftNotSaving(state[draftKey], 250, 5); // only one saveDraft at a time

        commit(mutationTypes.SET_MESSAGES_STATUS, [{ key: draftKey, status: MessageStatus.SAVING }]);

        const draft = state[draftKey];
        const service = inject("MailboxItemsPersistence", myDraftsFolderKey);
        const { saveDate, headers } = forceMailRewriteOnServer(draft);

        commit(mutationTypes.SET_MESSAGE_HEADERS, { messageKey: draft.key, headers });
        commit(mutationTypes.SET_MESSAGE_DATE, { messageKey: draft.key, date: saveDate });

        const { partsToUpload, inlineImages } = prepareDraft(draft, messageCompose, userPrefTextOnly, commit);

        const inlinePartAddresses = await uploadInlineParts(service, partsToUpload);
        const structure = createDraftStructure(draft, userPrefTextOnly, inlinePartAddresses, inlineImages);
        await service.updateById(draft.remoteRef.internalId, MessageAdaptor.realToMailboxItem(draft, structure));

        const newAttachments = draft.attachments.filter(attachment => attachment.address.length > 5); // new part addresses are uuid

        if (newAttachments.length > 0) {
            // needed to get new attachment addresses and new imapUid
            const message = (await apiMessages.multipleById([draft]))[0];
            message.key = draft.key;
            message.composing = true;
            commit(mutationTypes.ADD_MESSAGES, [message]);
        }

        commit(mutationTypes.SET_MESSAGES_STATUS, [{ key: draftKey, status: MessageStatus.LOADED }]);

        clean(
            inlinePartAddresses,
            newAttachments.map(a => a.address),
            service
        );
    } catch (e) {
        commit(mutationTypes.SET_MESSAGES_STATUS, [{ key: draftKey, status: MessageStatus.SAVE_ERROR }]);
        throw e;
    }
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
