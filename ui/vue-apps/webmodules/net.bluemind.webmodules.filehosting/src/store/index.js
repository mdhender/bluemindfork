import Vue from "vue";
import actions from "./actions";
import { SET_CONFIGURATION } from "./types/mutations";

const mutations = {
    [SET_CONFIGURATION](state, { autoDetachmentLimit, maxFilesize }) {
        Vue.set(state, "configuration", { autoDetachmentLimit, maxFilesize });
    },
    SET_UPLOADING_FILE(state, file) {
        Vue.set(state.uploadingFiles, file.key, file);
    }
};

export default {
    namespaced: false,
    state: { uploadingFiles: {}, configuration: null },
    mutations,
    actions
};
