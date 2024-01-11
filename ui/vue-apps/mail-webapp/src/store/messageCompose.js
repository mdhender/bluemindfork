import Vue from "vue";
import { inject } from "@bluemind/inject";
import { signatureUtils } from "@bluemind/mail";
import {
    DEBOUNCED_SET_MESSAGE_CONTENT,
    LOAD_MAX_MESSAGE_SIZE,
    REMOVE_ATTACHMENT,
    SET_DRAFT_CONTENT,
    SET_MESSAGE_CONTENT,
    TOGGLE_SIGNATURE,
    UPDATE_SIGNATURE
} from "~/actions";
import {
    ADD_FILE,
    RESET_COMPOSER,
    RESET_FILES,
    SET_DISCLAIMER,
    SET_DRAFT_COLLAPSED_CONTENT,
    SET_DRAFT_EDITOR_CONTENT,
    SET_FILE_ADDRESS,
    SET_FILE_HEADERS,
    SET_FILE_PROGRESS,
    SET_FILE_STATUS,
    SET_MAIL_TIPS,
    SET_MAX_MESSAGE_SIZE,
    SET_PERSONAL_SIGNATURE,
    SET_SIGNATURE,
    SHOW_SENDER
} from "~/mutations";
import { GET_DRAFT_CONTENT, IS_SENDER_SHOWN } from "~/getters";

import templateChooser from "./templateChooser";
const { removeCorporateSignatureContent, isCorporateSignature, isDisclaimer } = signatureUtils;

export default {
    mutations: {
        [RESET_COMPOSER]: state => {
            state.disclaimer = null;
            state.personalSignature = null;
            state.signature = null;
            state.editorContent = "";
            state.collapsedContent = null;
            state.inlineImagesSaved = [];
            state.isSenderShown = false;
            state.maxMessageSizeExceeded = false;
        },
        [SET_PERSONAL_SIGNATURE]: (state, signature) => {
            state.personalSignature = signature;
        },
        [SET_DISCLAIMER]: (state, mailTip) => {
            if (!mailTip || !state.disclaimer || mailTip.html !== state.disclaimer.html) {
                state.disclaimer = mailTip;
            }
        },
        [SET_DRAFT_EDITOR_CONTENT]: (state, content) => {
            state.editorContent = content;
        },
        [SET_DRAFT_COLLAPSED_CONTENT]: (state, collapsed) => {
            state.collapsedContent = collapsed;
        },
        [SET_MAX_MESSAGE_SIZE](state, size) {
            state.maxMessageSize = size;
        },
        [SHOW_SENDER]: (state, value) => {
            state.isSenderShown = value;
        },

        [SET_MAIL_TIPS]: (state, mailTips) => {
            state.mailTips = mailTips;
        },
        SET_SAVE_ERROR: (state, error) => {
            state.maxMessageSizeExceeded = error?.data?.errorCode === "ENTITY_TOO_LARGE";
        },
        [RESET_FILES]: state => {
            state.uploadingFiles = {};
        },
        [ADD_FILE]: (state, { file }) => {
            Vue.set(state.uploadingFiles, file.key, file);
        },
        [SET_FILE_PROGRESS]: (state, { key, progress }) => {
            updateUploadingFiles(state, key, { progress });
        },
        [SET_FILE_STATUS]: (state, { key, status }) => {
            updateUploadingFiles(state, key, { status });
        },
        [SET_FILE_HEADERS]: (state, { key, headers }) => {
            updateUploadingFiles(state, key, { headers });
        },
        [SET_FILE_ADDRESS]: (state, { key, address }) => {
            updateUploadingFiles(state, key, { address });
        },

        // Listeners
        [REMOVE_ATTACHMENT]: (state, { address }) => {
            const index = Object.values(state.uploadingFiles).findIndex(file => address === file.address);
            if (index > -1) {
                const key = Object.keys(state.uploadingFiles)[index];
                Vue.delete(state.uploadingFiles, key);
            }
        },

        [SET_SIGNATURE]: (state, payload) => {
            state.signature = payload;
        }
    },

    actions: {
        async [LOAD_MAX_MESSAGE_SIZE]({ commit }, userId) {
            const { messageMaxSize } = await inject("MailboxesPersistence").getMailboxConfig(userId);
            // take into account the email base64 encoding : 33% more space
            commit(SET_MAX_MESSAGE_SIZE, messageMaxSize / 1.33);
        },
        [SET_DRAFT_CONTENT]: ({ commit, getters, dispatch }, { draft, html, debounce }) => {
            commit(SET_DRAFT_EDITOR_CONTENT, html);
            const content = getters[GET_DRAFT_CONTENT];
            const action = debounce === false ? SET_MESSAGE_CONTENT : DEBOUNCED_SET_MESSAGE_CONTENT;
            return dispatch(action, { message: draft, content });
        },
        [UPDATE_SIGNATURE]({ commit, state }, { mailTips, signByDefault }) {
            if (Array.isArray(mailTips)) {
                if (mailTips.length > 0) {
                    const matchingTips = mailTips[0].matchingTips;
                    const disclaimer = matchingTips.find(isDisclaimer);

                    commit(SET_DISCLAIMER, disclaimer ? JSON.parse(disclaimer.value) : null);

                    const corporateSignature = matchingTips.find(isCorporateSignature);
                    if (corporateSignature) {
                        commit(SET_SIGNATURE, JSON.parse(corporateSignature.value));
                    } else {
                        commit(SET_SIGNATURE, state.personalSignature);
                    }
                } else {
                    commit(SET_DISCLAIMER, null);
                    commit(SET_SIGNATURE, signByDefault ? state.personalSignature : null);
                }
            } else {
                commit(SET_SIGNATURE, mailTips);
            }
        },
        [TOGGLE_SIGNATURE]({ state, commit }) {
            commit(SET_SIGNATURE, state.signature === null ? state.personalSignature : null);
        }
    },

    getters: {
        [GET_DRAFT_CONTENT]: ({ collapsedContent, editorContent, signature, disclaimer }) => {
            const wholeContent = collapsedContent ? editorContent + collapsedContent : editorContent;
            const corporateSignature = signature?.uid ? signature : null;
            return removeCorporateSignatureContent(wholeContent, { corporateSignature, disclaimer });
        },
        [IS_SENDER_SHOWN]: state => userSettings => state.isSenderShown || userSettings.always_show_from === "true",
        signature: state => state.signature
    },

    state: {
        disclaimer: null,
        personalSignature: null,
        signature: null,
        editorContent: "",
        collapsedContent: null,
        inlineImagesSaved: [],
        maxMessageSize: 0, // FIXME: it's a cross-composer data, it must be moved in another store
        isSenderShown: false,
        maxMessageSizeExceeded: false,
        showFormattingToolbar: false,
        synced: ["showFormattingToolbar"],
        mailTips: [],
        uploadingFiles: {}
    },
    modules: {
        templateChooser
    }
};

function updateUploadingFiles(state, key, update) {
    if (!state.uploadingFiles[key]) {
        Vue.set(state.uploadingFiles, key, {});
    }
    const file = state.uploadingFiles[key];
    Object.keys(update).forEach(keyEntry => {
        const value = update[keyEntry];
        if (!(keyEntry in file)) {
            Vue.set(file, keyEntry, value);
        }
        file[keyEntry] = value;
    });
}
