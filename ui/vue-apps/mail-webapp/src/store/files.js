import Vue from "vue";
import {
    SET_FILE_STATUS,
    ADD_FILE,
    ADD_FILES,
    REMOVE_FILE,
    SET_FILE_ADDRESS,
    SET_FILE_HEADERS,
    SET_FILE_PROGRESS,
    SET_FILE_URL
} from "~/mutations";
import { SET_FILES } from "~/actions";
const state = {};

const mutations = {
    [ADD_FILE]: (state, { file }) => {
        Vue.set(state, file.key, file);
    },
    [ADD_FILES]: (state, { files }) => {
        files.forEach(file => Vue.set(state, file.key, file));
    },

    [REMOVE_FILE]: (state, { key }) => {
        if (state[key]?.url?.startsWith("blob:")) {
            URL.revokeObjectURL(state[key].url);
        }
        Vue.delete(state, key);
    },
    [SET_FILE_PROGRESS]: (state, { key, loaded, total }) => {
        state[key].progress = { loaded, total };
    },
    [SET_FILE_STATUS]: (state, { key, status }) => {
        state[key].status = status;
    },
    [SET_FILE_URL]: (state, { key, url }) => {
        state[key].url = url;
    },
    [SET_FILE_ADDRESS]: (state, { key, address }) => {
        state[key].address = address;
    },
    [SET_FILE_HEADERS]: (state, { key, headers }) => {
        state[key].headers = headers;
    }
};

const actions = {
    [SET_FILES]: ({ state, commit }, { files }) => {
        Object.keys(state).forEach(key => commit(REMOVE_FILE, { key }));
        commit(ADD_FILES, { files });
    }
};

export default { state, mutations, actions };
