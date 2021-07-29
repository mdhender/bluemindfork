import { inject } from "@bluemind/inject";
import global from "@bluemind/global";
import UUIDGenerator from "@bluemind/uuid";

import { create, AttachmentStatus } from "~/model/attachment";
import { createFromFile as createPartFromFile } from "~/model/part";
import { DEBOUNCED_SAVE_MESSAGE } from "~/actions";
import {
    ADD_ATTACHMENT,
    REMOVE_ATTACHMENT,
    SET_ATTACHMENT_ADDRESS,
    SET_MESSAGE_HAS_ATTACHMENT,
    SET_ATTACHMENT_PROGRESS,
    SET_ATTACHMENT_STATUS
} from "~/mutations";

export default async function ({ commit, dispatch }, { draft, files, messageCompose }) {
    if (files.length > 0) {
        const promises = [];
        for (let file of files) {
            promises.push(addAttachment(commit, draft, file));
        }
        await Promise.all(promises);

        dispatch(DEBOUNCED_SAVE_MESSAGE, { draft, messageCompose });
    }
}

async function addAttachment(commit, draft, file) {
    const part = createPartFromFile(UUIDGenerator.generate(), file);
    const attachment = create(part, AttachmentStatus.NOT_LOADED);

    // this will contain a function for cancelling the upload, do not store it in Vuex
    global.cancellers = global.cancellers || {};
    global.cancellers[attachment.address + draft.key] = { cancel: undefined };

    try {
        // this will make the attachment component appear in the UI
        commit(ADD_ATTACHMENT, { messageKey: draft.key, attachment });

        commit(SET_MESSAGE_HAS_ATTACHMENT, { key: draft.key, hasAttachment: true });

        const service = inject("MailboxItemsPersistence", draft.folderRef.uid);
        const address = await service.uploadPart(
            file,
            global.cancellers[attachment.address + draft.key],
            createOnUploadProgress(commit, draft.key, attachment)
        );

        commit(SET_ATTACHMENT_ADDRESS, {
            messageKey: draft.key,
            oldAddress: attachment.address,
            address
        });
        commit(SET_ATTACHMENT_STATUS, {
            messageKey: draft.key,
            address: attachment.address,
            status: AttachmentStatus.UPLOADED
        });
    } catch (event) {
        const error = event.target && event.target.error ? event.target.error : event;
        handleError(commit, draft, error, attachment);
    }
}

function createOnUploadProgress(commit, messageKey, attachment) {
    return progress => {
        commit(SET_ATTACHMENT_PROGRESS, {
            messageKey,
            address: attachment.address,
            loaded: progress.loaded,
            total: progress.total
        });
    };
}

function handleError(commit, draft, error, attachment) {
    if (error.message === "CANCELLED_BY_CLIENT") {
        commit(REMOVE_ATTACHMENT, { messageKey: draft.key, address: attachment.address });
        commit(SET_MESSAGE_HAS_ATTACHMENT, {
            key: draft.key,
            hasAttachment: draft.attachments.length > 0
        });
    } else {
        commit(SET_ATTACHMENT_PROGRESS, {
            messageKey: draft.key,
            address: attachment.address,
            loaded: 100,
            total: 100
        });
        commit(SET_ATTACHMENT_STATUS, {
            messageKey: draft.key,
            address: attachment.address,
            status: AttachmentStatus.ERROR
        });
    }
}
