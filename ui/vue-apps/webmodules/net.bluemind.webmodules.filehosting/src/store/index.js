import Vue from "vue";
import actions from "./actions";
import { fileUtils } from "@bluemind/mail";
import { GET_FH_FILE } from "./types/getters";
import { SET_CONFIGURATION } from "./types/mutations";
const { FileStatus } = fileUtils;

const mutations = {
    [SET_CONFIGURATION](state, { autoDetachmentLimit, maxFilesize }) {
        Vue.set(state, "configuration", { autoDetachmentLimit, maxFilesize });
    },
    // Listeners
    REMOVE_file(state, { key }) {
        if (state.values[key]) {
            delete state.values[key];
        }
    },
    ADD_FILES(state, { files }) {
        files.forEach(file => addFile(state, { file }));
    },
    ADD_FILE: addFile,
    SET_FILE_ADDRESS(state, { key, address }) {
        if (state.values[key]) {
            Vue.set(state.values[key], "address", address);
        }
    },
    SET_FILE_HEADERS(state, { key, headers }) {
        addFile(state, { file: { key, headers } });
    }
};
const getters = {
    [GET_FH_FILE](state) {
        return ({ key }) => state.values[key];
    }
};

export default {
    namespaced: false,
    state: { values: {}, configuration: null },
    mutations,
    actions,
    getters
};

function addFile(state, { file: { key, headers } }) {
    let header =
        headers.find(header => header.name.toLowerCase() === "x-bm-disposition") ||
        headers.find(header => header.name.toLowerCase() === "x-mozilla-cloud-part");

    if (header) {
        const data = extractFileHostingInfos(header);

        if (!data.name) {
            let contentDispoHeader = headers.find(header => header.name.toLowerCase() === "content-disposition");
            const contentDispoData = extractFileHostingInfos(contentDispoHeader);
            data.name = contentDispoData.filename;
        }
        if (data.expirationDate && data.expirationDate < Date.now()) {
            data.status = FileStatus.INVALID;
        }
        Vue.set(state.values, key, data);
    }
}

function extractFileHostingInfos(header) {
    if (!header) {
        return {};
    }
    return Object.fromEntries(
        header.values[0]
            .split(";")
            .slice(1)
            .map(s => s.match(/ *([^=]*)=(.*)/).slice(1, 3))
    );
}
