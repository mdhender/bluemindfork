import { inject } from "@bluemind/inject";
import global from "@bluemind/global";
import UUIDGenerator from "@bluemind/uuid";

import { create, AttachmentStatus } from "../../../model/attachment";
import { SAVE_MESSAGE } from "~actions";
import {
    ADD_ATTACHMENT,
    REMOVE_ATTACHMENT,
    SET_ATTACHMENT_ADDRESS,
    SET_ATTACHMENT_PROGRESS,
    SET_ATTACHMENT_STATUS
} from "~mutations";

export default async function ({ commit, dispatch, state }, { messageKey, files, userPrefTextOnly, messageCompose }) {
    const myDraftsFolderRef = state[messageKey].folderRef;
    if (files.length > 0) {
        const promises = [];
        for (let file of files) {
            promises.push(addAttachment({ commit }, messageKey, file, myDraftsFolderRef.uid));
        }
        await Promise.all(promises);

        return dispatch(SAVE_MESSAGE, {
            userPrefTextOnly,
            draftKey: messageKey,
            myDraftsFolderKey: myDraftsFolderRef.key,
            messageCompose
        });
    }
}

async function addAttachment({ commit }, messageKey, file, myDraftsFolderUid) {
    const attachment = create(
        UUIDGenerator.generate(),
        null,
        file.name,
        null,
        file.type || "application/octet-stream",
        file.size,
        false
    );
    attachment.contentUrl = URL.createObjectURL(file);

    // this will contain a function for cancelling the upload, do not store it in Vuex
    global.cancellers = global.cancellers || {};
    global.cancellers[attachment.address + messageKey] = { cancel: undefined };

    try {
        // this will make the attachment component appear in the UI
        commit(ADD_ATTACHMENT, { messageKey, attachment });

        const address = await inject("MailboxItemsPersistence", myDraftsFolderUid).uploadPart(
            file,
            global.cancellers[attachment.address + messageKey],
            createOnUploadProgress(commit, messageKey, attachment)
        );

        commit(SET_ATTACHMENT_ADDRESS, {
            messageKey,
            oldAddress: attachment.address,
            address
        });
    } catch (event) {
        const error = event.target && event.target.error ? event.target.error : event;
        handleError(commit, error, attachment, messageKey);
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

function handleError(commit, error, attachment, messageKey) {
    if (error.message === "CANCELLED_BY_CLIENT") {
        commit(REMOVE_ATTACHMENT, { messageKey, address: attachment.address });
    } else {
        commit(SET_ATTACHMENT_PROGRESS, {
            messageKey,
            address: attachment.address,
            loaded: 100,
            total: 100
        });
        commit(SET_ATTACHMENT_STATUS, {
            messageKey,
            address: attachment.address,
            status: AttachmentStatus.ERROR
        });
    }
}
