import Vue from "vue";
import { inject } from "@bluemind/inject";
import { signatureUtils } from "@bluemind/mail";
import { GET_DRAFT_CONTENT, IS_SENDER_SHOWN, SIGNATURE, DISCLAIMER } from "~/getters";
import {
    ADD_FILE,
    RESET_COMPOSER,
    RESET_FILES,
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
    SHOW_SENDER,
    SIGNATURE_TOGGLED
} from "~/mutations";
import {
    DEBOUNCED_SET_MESSAGE_CONTENT,
    LOAD_MAX_MESSAGE_SIZE,
    REMOVE_ATTACHMENT,
    SET_DRAFT_CONTENT,
    SET_MESSAGE_CONTENT,
    TOGGLE_SIGNATURE
} from "~/actions";
import templateChooser from "./templateChooser";

const {
    removeCorporateSignatureContent,
    isCorporateSignature,
    isDisclaimer,
    wrapCorporateSignature,
    wrapDisclaimer,
    wrapPersonalSignature
} = signatureUtils;

export default {
    modules: {
        templateChooser
    },
    state: {
        personalSignature: { toggleStatus: false },
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

    mutations: {
        [RESET_COMPOSER]: state => {
            state.personalSignature = { toggleStatus: false };
            state.editorContent = "";
            state.collapsedContent = null;
            state.inlineImagesSaved = [];
            state.isSenderShown = false;
            state.maxMessageSizeExceeded = false;
        },
        [SET_PERSONAL_SIGNATURE]: (state, signature) => {
            state.personalSignature = { toggleStatus: state.personalSignature.toggleStatus, ...signature };
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
        },
        [SIGNATURE_TOGGLED]: (state, payload) => {
            state.personalSignature.toggleStatus = payload;
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
        [TOGGLE_SIGNATURE]({ state, commit }) {
            commit(SIGNATURE_TOGGLED, !state.personalSignature.toggleStatus);
        }
    },

    getters: {
        [GET_DRAFT_CONTENT]: ({ collapsedContent, editorContent, disclaimer }, getters) => {
            const signature = getters[SIGNATURE];
            const corporateSignature = signature?.uid ? signature : null;

            return removeCorporateSignatureContent(
                collapsedContent ? editorContent + collapsedContent : editorContent,
                { corporateSignature, disclaimer }
            );
        },
        [IS_SENDER_SHOWN]: state => userSettings => state.isSenderShown || userSettings.always_show_from === "true",

        [SIGNATURE]: (state, getters) => {
            if (!state.mailTips.length && state.personalSignature) {
                return getters["__fallbackSignature"];
            }

            const matchingTips = state.mailTips[0]?.matchingTips || [];
            const corporateSignature = matchingTips.find(isCorporateSignature);
            if (!corporateSignature) {
                return getters["__fallbackSignature"];
            }

            const jsonCorporateSignature = JSON.parse(corporateSignature.value);
            return {
                ...jsonCorporateSignature,
                html: wrapCorporateSignature(jsonCorporateSignature.html)
            };
        },
        __fallbackSignature: state => {
            if (state.personalSignature.toggleStatus) {
                return {
                    id: state.personalSignature.id,
                    html: wrapPersonalSignature({
                        html: state.personalSignature?.html,
                        id: state.personalSignature?.id
                    })
                };
            }
            return null;
        },
        [DISCLAIMER]: state => {
            if (!state.mailTips.length) {
                return null;
            }

            const matchingTips = state.mailTips[0].matchingTips;
            const disclaimer = matchingTips.find(isDisclaimer)?.value;

            if (!disclaimer) {
                return null;
            }

            return wrapDisclaimer(JSON.parse(disclaimer).html);
        }
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
