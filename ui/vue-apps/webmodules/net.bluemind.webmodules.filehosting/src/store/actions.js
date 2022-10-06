import { inject } from "@bluemind/inject";
import { withAlert } from "./helpers";
import addFhAttachment from "./addFhAttachment";
import { ADD_FH_ATTACHMENT, LINK_FH_ATTACHMENT, SHARE_ATTACHMENT, GET_CONFIGURATION } from "./types/actions";
import { SET_CONFIGURATION } from "./types/mutations";

async function uploadFh({ fileName, key }, content, commit) {
    const serviceAttachment = inject("AttachmentPersistence");
    global.cancellers = global.cancellers || {};
    global.cancellers[key] = { cancel: undefined };
    const { publicUrl, name, expirationDate } = await serviceAttachment.share(
        encodeURIComponent(fileName),
        content,
        global.cancellers[key],
        createOnUploadProgress(commit, key)
    );
    return { url: publicUrl, name, expirationDate };
}

async function getPublicurl({ fileName, path }) {
    const { url, expirationDate } = await inject("FileHostingPersistence").share(path);
    return { name: fileName, url, expirationDate };
}

function createOnUploadProgress(commit, fileKey) {
    return progress => {
        commit("SET_FILE_PROGRESS", {
            key: fileKey,
            loaded: progress.loaded,
            total: progress.total
        });
    };
}

async function share({ commit }, file) {
    const content = await fetch(file.url);
    const blob = await content.blob();
    return await uploadFh(file, blob, commit);
}

export default {
    async [ADD_FH_ATTACHMENT]({ commit }, { file, message }) {
        return await addFhAttachment({ commit }, { file, message, shareFn: uploadFh });
    },
    async [LINK_FH_ATTACHMENT]({ commit }, { file, message }) {
        return await addFhAttachment({ commit }, { file, message, shareFn: getPublicurl });
    },
    [SHARE_ATTACHMENT]: withAlert(share, SHARE_ATTACHMENT),
    async [GET_CONFIGURATION]({ state, commit }) {
        if (!state.configuration) {
            const config = await inject("AttachmentPersistence").getConfiguration();
            commit(SET_CONFIGURATION, config);
        }
        return state.configuration;
    }
};
