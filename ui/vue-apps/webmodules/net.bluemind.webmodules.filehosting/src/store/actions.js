import { ProgressMonitor } from "@bluemind/api.commons";
import { inject } from "@bluemind/inject";
import { withAlert } from "./helpers";
import addFhAttachment from "./addFhAttachment";
import addFhFile from "./addFhFile";
import detachAttachment from "./detachAttachment";
import {
    ADD_FH_ATTACHMENT,
    ADD_FH_FILE,
    DETACH_ATTACHMENT,
    GET_CONFIGURATION,
    LINK_FH_ATTACHMENT,
    REMOVE_FH_ATTACHMENT,
    SHARE_ATTACHMENT
} from "./types/actions";
import { SET_CONFIGURATION } from "./types/mutations";

const AbortControllers = new Map();

async function uploadFh({ fileName, key }, content, commit) {
    const serviceAttachment = inject("AttachmentPersistence");
    const canceller = new AbortController();
    AbortControllers.set(key, canceller);
    const monitor = new ProgressMonitor();
    monitor.addEventListener("progress", onUploadProgressMonitor(commit, key), {
        mode: ProgressMonitor.UPLOAD
    });
    const { publicUrl, name, expirationDate } = await serviceAttachment.share(encodeURIComponent(fileName), content, {
        signal: canceller.signal,
        monitor
    });
    return { url: publicUrl, name, expirationDate };
}

async function cancelFhAttachment(context, { key }) {
    if (AbortControllers.has(key)) {
        AbortControllers.get(key).abort();
    }
}

async function getPublicurl({ fileName, path }) {
    const { url, expirationDate } = await inject("FileHostingPersistence").share(path);
    return { name: fileName, url, expirationDate };
}

function onUploadProgressMonitor(commit, fileKey) {
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
    async [ADD_FH_ATTACHMENT]({ commit, dispatch }, { file, message }) {
        return await addFhAttachment({ commit, dispatch }, { file, message, shareFn: uploadFh });
    },
    [REMOVE_FH_ATTACHMENT]: cancelFhAttachment,
    async [LINK_FH_ATTACHMENT]({ commit, dispatch }, { file, message }) {
        return await addFhAttachment({ commit, dispatch }, { file, message, shareFn: getPublicurl });
    },
    async [ADD_FH_FILE]({ commit }, { file, message, content, shareFn = uploadFh }) {
        return await addFhFile({ commit }, { file, message, content, shareFn });
    },
    [SHARE_ATTACHMENT]: withAlert(share, SHARE_ATTACHMENT),
    [DETACH_ATTACHMENT]: withAlert(detachAttachment, DETACH_ATTACHMENT),
    async [GET_CONFIGURATION]({ state, commit }) {
        if (!state.configuration) {
            const config = await inject("AttachmentPersistence").getConfiguration();
            commit(SET_CONFIGURATION, config);
        }
        return state.configuration;
    }
};
