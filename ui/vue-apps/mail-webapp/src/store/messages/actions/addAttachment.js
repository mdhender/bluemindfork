import { inject } from "@bluemind/inject";
import global from "@bluemind/global";
import { AttachmentStatus } from "~/model/attachment";

import {
    ADD_ATTACHMENT,
    REMOVE_ATTACHMENT,
    SET_ATTACHMENT_ADDRESS,
    SET_MESSAGE_HAS_ATTACHMENT,
    SET_ATTACHMENT_PROGRESS,
    SET_ATTACHMENT_STATUS
} from "~/mutations";

export default async function ({ commit }, { message, attachment, content }) {
    commit(ADD_ATTACHMENT, { messageKey: message.key, attachment });
    global.cancellers = global.cancellers || {};
    global.cancellers[attachment.address + message.key] = { cancel: undefined };

    commit(SET_ATTACHMENT_STATUS, {
        messageKey: message.key,
        address: attachment.address,
        status: AttachmentStatus.NOT_UPLOADED
    });

    commit(SET_MESSAGE_HAS_ATTACHMENT, { key: message.key, hasAttachment: true });

    try {
        const service = inject("MailboxItemsPersistence", message.folderRef.uid);
        const address = await service.uploadPart(
            content,
            global.cancellers[attachment.address + message.key],
            createOnUploadProgress(commit, message.key, attachment)
        );

        commit(SET_ATTACHMENT_ADDRESS, {
            messageKey: message.key,
            oldAddress: attachment.address,
            address
        });
        commit(SET_ATTACHMENT_STATUS, {
            messageKey: message.key,
            address: attachment.address,
            status: AttachmentStatus.UPLOADED
        });
    } catch (event) {
        const error = event.target && event.target.error ? event.target.error : event;
        handleError(commit, message, error, attachment);
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
