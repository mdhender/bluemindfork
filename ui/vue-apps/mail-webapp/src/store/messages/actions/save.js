import { inject } from "@bluemind/inject";

import apiMessages from "../../api/apiMessages";
import debounce from "lodash/debounce";
import MessageAdaptor from "../helpers/MessageAdaptor";
import { MessageStatus, clean } from "../../../model/message";
import { createDraftStructure, forceMailRewriteOnServer, prepareDraft } from "../../../model/draft";
import mutationTypes from "../../mutationTypes";

let debouncedSave = { cancel: () => {} };

export default async function save(
    context,
    { userPrefTextOnly, draftKey, myDraftsFolderKey, messageCompose, debounceTime }
) {
    debouncedSave.cancel();
    return new Promise(resolve => {
        debouncedSave = debounce(
            () => resolve(doSave(context, userPrefTextOnly, draftKey, myDraftsFolderKey, messageCompose)),
            debounceTime
        );
        debouncedSave();
    });
}

async function doSave(context, userPrefTextOnly, draftKey, myDraftsFolderKey, messageCompose) {
    try {
        await waitUntilDraftNotSaving(context.state, draftKey, 250, 5); // only one saveDraft at a time
        const draft = context.state[draftKey];

        if (!draft) {
            // draft may have been deleted
            return;
        }

        context.commit(mutationTypes.SET_MESSAGES_STATUS, [{ key: draftKey, status: MessageStatus.SAVING }]);

        const service = inject("MailboxItemsPersistence", myDraftsFolderKey);
        const { saveDate, headers } = forceMailRewriteOnServer(draft);

        context.commit(mutationTypes.SET_MESSAGE_HEADERS, { messageKey: draft.key, headers });
        context.commit(mutationTypes.SET_MESSAGE_DATE, { messageKey: draft.key, date: saveDate });

        const { partsToUpload, inlineImages } = prepareDraft(draft, messageCompose, userPrefTextOnly, context.commit);

        const inlinePartAddresses = await uploadInlineParts(service, partsToUpload);
        const structure = createDraftStructure(draft, userPrefTextOnly, inlinePartAddresses, inlineImages);
        await service.updateById(draft.remoteRef.internalId, MessageAdaptor.realToMailboxItem(draft, structure));

        const newAttachments = draft.attachments.filter(attachment => attachment.address.length > 5); // new part addresses are uuid

        if (newAttachments.length > 0) {
            // needed to get new attachment addresses and new imapUid
            const message = (await apiMessages.multipleById([draft]))[0];
            message.key = draft.key;
            message.composing = true;
            context.commit(mutationTypes.ADD_MESSAGES, [message]);
        }

        context.commit(mutationTypes.SET_MESSAGES_STATUS, [{ key: draftKey, status: MessageStatus.LOADED }]);

        clean(
            inlinePartAddresses,
            newAttachments.map(a => a.address),
            service
        );
    } catch (e) {
        context.commit(mutationTypes.SET_MESSAGES_STATUS, [{ key: draftKey, status: MessageStatus.SAVE_ERROR }]);
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
function waitUntilDraftNotSaving(state, draftKey, delayTime, maxTries, iteration = 1) {
    const draft = state[draftKey];
    if (draft && draft.status === MessageStatus.SAVING) {
        return new Promise(resolve => setTimeout(() => resolve(draft.status), delayTime)).then(status => {
            if (status !== MessageStatus.SAVING) {
                return Promise.resolve();
            } else {
                if (iteration < maxTries) {
                    // 'smart' delay: add 250ms each retry
                    return waitUntilDraftNotSaving(state, draftKey, delayTime + 250, maxTries, ++iteration);
                } else {
                    return Promise.reject("Timeout while waiting for the draft to be saved");
                }
            }
        });
    } else {
        return Promise.resolve();
    }
}
