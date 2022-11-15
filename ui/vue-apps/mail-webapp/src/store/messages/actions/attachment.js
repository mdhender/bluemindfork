import { ProgressMonitor } from "@bluemind/api.commons";
import { inject } from "@bluemind/inject";
import { attachmentUtils, fileUtils } from "@bluemind/mail";
import { getPartDownloadUrl } from "@bluemind/email";

import { DEBOUNCED_SAVE_MESSAGE } from "~/actions";

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

const AbortControllers = new Map();

export async function addAttachment({ commit }, { message, attachment, content }) {
    const { attachments, files } = AttachmentAdaptor.extractFiles([attachment], message);
    const file = files.pop();
    const attachmentInfos = attachments.pop();

    commit(ADD_FILE, { file });
    commit(ADD_ATTACHMENT, { messageKey: message.key, attachment: attachmentInfos });
    const canceller = new AbortController();
    AbortControllers.set(file.key, canceller);

    commit(SET_FILE_STATUS, { key: attachmentInfos.fileKey, status: FileStatus.NOT_LOADED });
    commit(SET_MESSAGE_HAS_ATTACHMENT, { key: message.key, hasAttachment: true });

    try {
        const service = inject("MailboxItemsPersistence", message.folderRef.uid);
        const monitor = new ProgressMonitor();
        monitor.addEventListener("progress", onUploadProgressMonitor(commit, attachmentInfos), {
            mode: ProgressMonitor.UPLOAD
        });
        const address = await service.uploadPart(content, { signal: canceller.signal, monitor });
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
    } finally {
        AbortControllers.delete(file.key);
    }
}

export function addLocalAttachment({ commit }, { message, attachment, content }) {
    const { attachments, files } = AttachmentAdaptor.extractFiles([attachment], message);
    const file = files.pop();
    const attachmentInfos = attachments.pop();

    if (content instanceof ArrayBuffer ? !content.byteLength : !content.length) {
        file.url = null;
        file.size = 0;
    } else {
        const fileBlob = new File([content], file.fileName, { type: file.mime });
        file.url = URL.createObjectURL(fileBlob);
        file.size = fileBlob.size;
    }

    commit(ADD_FILE, { file });
    commit(REMOVE_ATTACHMENT, { messageKey: message.key, address: attachment.address });
    commit(ADD_ATTACHMENT, { messageKey: message.key, attachment: attachmentInfos });
    commit(SET_FILE_STATUS, { key: attachmentInfos.fileKey, status: FileStatus.ONLY_LOCAL });
    commit(SET_MESSAGE_HAS_ATTACHMENT, { key: message.key, hasAttachment: true });
}

function handleError(commit, draft, error, attachment) {
    if (error.name === "AbortError") {
        commit(REMOVE_ATTACHMENT, { messageKey: draft.key, address: attachment.address });
        commit("REMOVE_FILE", { key: attachment.fileKey });
        commit("SET_MESSAGE_HAS_ATTACHMENT", {
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

export async function removeAttachment({ commit, dispatch, state }, { messageKey, attachment, messageCompose }) {
    const draft = state[messageKey];
    if (AbortControllers.has(attachment.fileKey)) {
        AbortControllers.get(attachment.fileKey).abort();
    } else {
        commit(REMOVE_FILE, { key: attachment.fileKey });
        commit(REMOVE_ATTACHMENT, { messageKey, address: attachment.address });
        commit(SET_MESSAGE_HAS_ATTACHMENT, {
            key: messageKey,
            hasAttachment: draft.attachments.length > 0
        });
        dispatch(DEBOUNCED_SAVE_MESSAGE, { draft, messageCompose });
        inject("MailboxItemsPersistence", draft.folderRef.uid).removePart(attachment.address);
    }
}

function onUploadProgressMonitor(commit, attachment) {
    return progress => {
        commit(SET_FILE_PROGRESS, {
            key: attachment.fileKey,
            loaded: progress.loaded,
            total: progress.total
        });
    };
}
