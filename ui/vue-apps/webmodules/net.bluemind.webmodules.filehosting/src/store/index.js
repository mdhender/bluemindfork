import Vue from "vue";
import actions from "./actions";

const mutations = {
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
            Vue.set(state.values[key], address, address);
        }
    },
    SET_FILE_HEADERS(state, { key, headers }) {
        addFile(state, { file: { key, headers } });
    }
};
const getters = {
    GET_FH_FILE(state) {
        return ({ key }) => state.values[key];
    }
};

export default {
    namespaced: false,
    state: { values: {} },
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
