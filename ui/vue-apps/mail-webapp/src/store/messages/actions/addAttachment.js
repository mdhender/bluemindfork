import { inject } from "@bluemind/inject";
import { attachmentUtils, fileUtils } from "@bluemind/mail";
import global from "@bluemind/global";
import { getPartDownloadUrl } from "@bluemind/email";

import {
    ADD_ATTACHMENT,
    ADD_FILE,
    REMOVE_ATTACHMENT,
    REMOVE_FILE,
    SET_ATTACHMENT_ADDRESS,
    SET_FILE_ADDRESS,
    SET_FILE_PROGRESS,
    SET_FILE_STATUS,
    SET_FILE_URL,
    SET_MESSAGE_HAS_ATTACHMENT
} from "~/mutations";

const { AttachmentAdaptor } = attachmentUtils;
const { FileStatus } = fileUtils;

export default async function ({ commit }, { message, attachment, content }) {
    const { attachments, files } = AttachmentAdaptor.extractFiles([attachment], message);
    const file = files.pop();
    const attachmentInfos = attachments.pop();

    commit(ADD_FILE, { file });
    commit(ADD_ATTACHMENT, { messageKey: message.key, attachment: attachmentInfos });
    global.cancellers = global.cancellers || {};
    global.cancellers[file.key] = { cancel: undefined };

    commit(SET_FILE_STATUS, {
        key: attachmentInfos.fileKey,
        status: FileStatus.NOT_LOADED
    });

    commit(SET_MESSAGE_HAS_ATTACHMENT, { key: message.key, hasAttachment: true });

    try {
        const service = inject("MailboxItemsPersistence", message.folderRef.uid);
        const address = await service.uploadPart(
            content,
            global.cancellers[file.key],
            createOnUploadProgress(commit, attachmentInfos)
        );

        commit(SET_ATTACHMENT_ADDRESS, {
            messageKey: message.key,
            oldAddress: attachmentInfos.address,
            address
        });
        commit(SET_FILE_URL, {
            key: attachmentInfos.fileKey,
            url: getPartDownloadUrl(message.folderRef.uid, message.remoteRef.imapUid, {
                ...file,
                address
            })
        });
        commit(SET_FILE_STATUS, {
            key: attachmentInfos.fileKey,
            status: FileStatus.UPLOADED
        });
        commit(SET_FILE_ADDRESS, {
            key: attachmentInfos.fileKey,
            address
        });
    } catch (event) {
        const error = event.target && event.target.error ? event.target.error : event;
        handleError(commit, message, error, attachmentInfos);
    }
}

function createOnUploadProgress(commit, attachment) {
    return progress => {
        commit(SET_FILE_PROGRESS, {
            key: attachment.fileKey,
            loaded: progress.loaded,
            total: progress.total
        });
    };
}

function handleError(commit, draft, error, attachment) {
    if (error.message === "CANCELLED_BY_CLIENT") {
        commit(REMOVE_ATTACHMENT, { messageKey: draft.key, address: attachment.address });
        commit(REMOVE_FILE, { key: attachment.fileKey });
        commit(SET_MESSAGE_HAS_ATTACHMENT, {
            key: draft.key,
            hasAttachment: draft.attachments.length > 0
        });
    } else {
        commit(SET_FILE_PROGRESS, {
            key: attachment.fileKey,
            loaded: 100,
            total: 100
        });
        commit(SET_FILE_STATUS, {
            key: attachment.fileKey,
            status: FileStatus.ERROR
        });
    }
}
