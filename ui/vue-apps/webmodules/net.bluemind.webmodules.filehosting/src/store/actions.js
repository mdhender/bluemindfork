import { inject } from "@bluemind/inject";
import addFhAttachment from "./addFhAttachment";

export default {
    async ADD_FH_ATTACHMENT({ commit }, { file, message }) {
        return await addFhAttachment({ commit }, { file, message, shareFn: uploadFh });
    },
    async LINK_FH_ATTACHMENT({ commit }, { file, message }) {
        return await addFhAttachment({ commit }, { file, message, shareFn: getPublicurl });
    }
};

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
