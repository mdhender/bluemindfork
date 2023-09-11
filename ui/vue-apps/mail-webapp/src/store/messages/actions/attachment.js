import { ProgressMonitor } from "@bluemind/api.commons";
import { inject } from "@bluemind/inject";
import { attachmentUtils, fileUtils } from "@bluemind/mail";
import { PartsBuilder } from "@bluemind/email";

import { DEBOUNCED_SAVE_MESSAGE } from "~/actions";

import {
    ADD_ATTACHMENT,
    ADD_FILE,
    REMOVE_ATTACHMENT,
    SET_ATTACHMENT_ADDRESS,
    SET_FILE_ADDRESS,
    SET_FILE_PROGRESS,
    SET_FILE_STATUS
} from "~/mutations";

const { AttachmentAdaptor } = attachmentUtils;
const { FileStatus } = fileUtils;

const AbortControllers = new Map();

export async function addAttachment({ commit }, { message, attachment, content }) {
    const file = AttachmentAdaptor.extractFiles([attachment], message).pop();
    const attachmentPart = PartsBuilder.createAttachmentPart(file);
    commit(ADD_ATTACHMENT, { messageKey: message.key, attachment: attachmentPart });

    const canceller = new AbortController();
    AbortControllers.set(file.key, canceller);
    commit(ADD_FILE, { file });
    commit(SET_FILE_STATUS, {
        key: file.key,
        status: FileStatus.NOT_LOADED
    });
    try {
        const service = inject("MailboxItemsPersistence", message.folderRef.uid);
        const monitor = new ProgressMonitor();
        monitor.addEventListener("progress", onUploadProgressMonitor(commit, file), {
            mode: ProgressMonitor.UPLOAD
        });
        const address = await service.uploadPart(content, { signal: canceller.signal, monitor });

        commit(SET_ATTACHMENT_ADDRESS, {
            messageKey: message.key,
            oldAddress: attachmentPart.address,
            address: address
        });
        commit(SET_FILE_STATUS, {
            key: file.key,
            status: FileStatus.UPLOADED
        });
        commit(SET_FILE_ADDRESS, {
            key: file.key,
            address
        });
    } catch (event) {
        const error = event.target && event.target.error ? event.target.error : event;
        handleError(commit, message, error, attachmentPart, file.key);
    } finally {
        AbortControllers.delete(file.key);
    }
}

export function addLocalAttachment({ commit }, { message, attachment, content }) {
    const files = AttachmentAdaptor.extractFiles([attachment], message);
    const file = files.pop();
    const attachmentPart = PartsBuilder.createAttachmentPart(file);
    const newFile = {};
    if (content instanceof ArrayBuffer ? !content.byteLength : !content.length) {
        newFile.url = null;
        newFile.size = 0;
    } else {
        const fileBlob = new File([content], file.fileName, { type: file.mime });
        newFile.url = URL.createObjectURL(fileBlob);
        newFile.size = fileBlob.size;
    }

    commit(ADD_FILE, { file: newFile });
    commit(REMOVE_ATTACHMENT, { messageKey: message.key, address: attachmentPart.address });
    commit(ADD_ATTACHMENT, { messageKey: message.key, attachment: attachmentPart });
    commit(SET_FILE_STATUS, { key: file.key, status: FileStatus.ONLY_LOCAL });
}

function handleError(commit, draft, error, attachment, fileKey) {
    if (error.name === "AbortError") {
        commit(REMOVE_ATTACHMENT, { messageKey: draft.key, address: attachment.address });
    } else {
        commit(SET_FILE_PROGRESS, {
            key: fileKey,
            progress: { loaded: 100, total: 100 }
        });
        commit(SET_FILE_STATUS, { key: fileKey, status: FileStatus.ERROR });
    }
}

export async function removeAttachment({ commit, dispatch, state }, { messageKey, address }) {
    const draft = state[messageKey];
    if (AbortControllers.has(address)) {
        AbortControllers.get(address).abort();
    } else {
        commit(REMOVE_ATTACHMENT, { messageKey, address });
        dispatch(DEBOUNCED_SAVE_MESSAGE, { draft });
        inject("MailboxItemsPersistence", draft.folderRef.uid).removePart(address);
    }
}

function onUploadProgressMonitor(commit, file) {
    return progress => {
        commit(SET_FILE_PROGRESS, {
            key: file.key,
            progress: { loaded: progress.loaded, total: progress.total }
        });
    };
}
