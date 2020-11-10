import { inject } from "@bluemind/inject";

import debounce from "lodash/debounce";
import MessageAdaptor from "../helpers/MessageAdaptor";
import { MessageStatus, clean } from "../../../model/message";
import { createDraftStructure, forceMailRewriteOnServer, prepareDraft } from "../../../model/draft";
import { isTemporaryPart, setAddresses } from "../../../model/part";
import { isAttachment } from "../../../model/attachment";
import {
    SET_ATTACHMENT_ADDRESS,
    SET_MESSAGES_STATUS,
    SET_MESSAGE_DATE,
    SET_MESSAGE_HEADERS,
    SET_MESSAGE_INTERNAL_ID,
    SET_ATTACHMENT_ENCODING
} from "~mutations";

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

        context.commit(SET_MESSAGES_STATUS, [{ key: draftKey, status: MessageStatus.SAVING }]);

        const service = inject("MailboxItemsPersistence", myDraftsFolderKey);
        const { saveDate, headers } = forceMailRewriteOnServer(draft);

        context.commit(SET_MESSAGE_HEADERS, { messageKey: draft.key, headers });
        context.commit(SET_MESSAGE_DATE, { messageKey: draft.key, date: saveDate });

        const { partsToUpload, inlineImages } = prepareDraft(draft, messageCompose, userPrefTextOnly, context.commit);

        const inlinePartAddresses = await uploadInlineParts(service, partsToUpload);

        const structure = createDraftStructure(draft.attachments, userPrefTextOnly, inlinePartAddresses, inlineImages);
        const remoteMessage = MessageAdaptor.toMailboxItem(draft, structure);
        if (draft.remoteRef.internalId === "faked-internal-id") {
            const internalId = (await service.create(remoteMessage)).id;
            context.commit(SET_MESSAGE_INTERNAL_ID, { key: draftKey, internalId });
        } else {
            await service.updateById(draft.remoteRef.internalId, remoteMessage);
        }

        context.commit(SET_MESSAGES_STATUS, [{ key: draftKey, status: MessageStatus.LOADED }]);

        const newAttachments = [...draft.attachments.filter(isTemporaryPart)];
        await clean(inlinePartAddresses, newAttachments, service);
        updateAddresses(structure, draft.attachments, newAttachments, draftKey, context.commit);
    } catch (e) {
        context.commit(SET_MESSAGES_STATUS, [{ key: draftKey, status: MessageStatus.SAVE_ERROR }]);
        throw e;
    }
}

// FIXME: remove me once FEATWEBML-1253 is done ?
function updateAddresses(structure, attachments, newAttachments, draftKey, commit) {
    structure.children.forEach(part => {
        part.uid = part.address;
        part.address = "";
    });
    setAddresses(structure);
    structure.children.forEach(part => {
        if (part.uid !== part.address && isAttachment(part)) {
            commit(SET_ATTACHMENT_ADDRESS, {
                messageKey: draftKey,
                oldAddress: part.uid,
                address: part.address
            });
            // default values set by server
            commit(SET_ATTACHMENT_ENCODING, {
                messageKey: draftKey,
                address: part.address,
                charset: "us-ascii",
                encoding: "base64"
            });
        }
    });
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
