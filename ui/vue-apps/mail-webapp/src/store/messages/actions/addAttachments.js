import { inject } from "@bluemind/inject";
import global from "@bluemind/global";
import UUIDGenerator from "@bluemind/uuid";

import actionTypes from "../../actionTypes";
import { create, AttachmentStatus } from "../../../model/attachment";
import mutationTypes from "../../mutationTypes";

export default async function (
    { state, commit, dispatch },
    { messageKey, files, userPrefTextOnly, myDraftsFolderKey, editorContent }
) {
    console.log("1");
    if (files.length > 0) {
        const promises = [];
        for (let file of files) {
            promises.push(addAttachment({ state, commit }, messageKey, file, myDraftsFolderKey));
        }
        await Promise.all(promises);

        return dispatch(actionTypes.SAVE_MESSAGE, {
            userPrefTextOnly,
            draftKey: messageKey,
            myDraftsFolderKey,
            editorContent
        });
    }
}

async function addAttachment({ state, commit }, messageKey, file, myDraftsFolderKey) {
    // FIXME: need to set a charset and encoding ?
    const attachment = create(
        UUIDGenerator.generate(),
        null,
        file.name,
        null,
        file.type || "application/octet-stream",
        file.size,
        false
    );

    // this will contain a function for cancelling the upload, do not store it in Vuex
    global.cancellers = global.cancellers || {};
    global.cancellers[attachment.address + messageKey] = { cancel: undefined };

    try {
        // this will make the attachment component appear in the UI
        commit(mutationTypes.ADD_ATTACHMENT, { messageKey, attachment });

        const address = await inject("MailboxItemsPersistence", myDraftsFolderKey).uploadPart(
            file,
            global.cancellers[attachment.address + messageKey],
            createOnUploadProgress(commit, state, messageKey, attachment)
        );

        commit(mutationTypes.UPDATE_ATTACHMENT, {
            messageKey,
            oldAddress: attachment.address,
            address,
            contentUrl: URL.createObjectURL(file)
        });
    } catch (event) {
        const error = event.target && event.target.error ? event.target.error : event;
        handleError(commit, error, attachment, messageKey);
    }
}

function createOnUploadProgress(commit, state, messageKey, attachment) {
    return progress => {
        commit(mutationTypes.SET_ATTACHMENT_PROGRESS, {
            messageKey,
            address: attachment.address,
            loaded: progress.loaded,
            total: progress.total
        });
    };
}

function handleError(commit, error, attachment, messageKey) {
    if (error.message === "CANCELLED_BY_CLIENT") {
        commit(mutationTypes.REMOVE_ATTACHMENT, { messageKey, address: attachment.address });
    } else {
        commit(mutationTypes.SET_ATTACHMENT_PROGRESS, {
            messageKey,
            address: attachment.address,
            loaded: 100,
            total: 100
        });
        commit(mutationTypes.SET_ATTACHMENT_STATUS, {
            messageKey,
            address: attachment.address,
            status: AttachmentStatus.ERROR
        });
    }
}
