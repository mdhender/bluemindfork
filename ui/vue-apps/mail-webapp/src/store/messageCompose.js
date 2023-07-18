import { inject } from "@bluemind/inject";
import { LOAD_MAX_MESSAGE_SIZE } from "~/actions";
import {
    RESET_COMPOSER,
    SET_CORPORATE_SIGNATURE,
    SET_PERSONAL_SIGNATURE,
    SET_DISCLAIMER,
    SET_DRAFT_COLLAPSED_CONTENT,
    SET_DRAFT_EDITOR_CONTENT,
    SET_MAX_MESSAGE_SIZE,
    SET_MAIL_TIPS,
    SET_SAVED_INLINE_IMAGES,
    SHOW_SENDER,
    UNSET_CORPORATE_SIGNATURE
} from "~/mutations";
import { IS_SENDER_SHOWN } from "~/getters";
import templateChooser from "./templateChooser";

export default {
    mutations: {
        [RESET_COMPOSER]: state => {
            state.disclaimer = null;
            state.corporateSignature = null;
            state.personalSignature = null;
            state.editorContent = "";
            state.collapsedContent = null;
            state.inlineImagesSaved = [];
            state.isSenderShown = false;
            state.maxMessageSizeExceeded = false;
        },
        [SET_CORPORATE_SIGNATURE]: (state, mailTip) => {
            if (!state.corporateSignature || state.corporateSignature.html !== mailTip.html) {
                state.corporateSignature = mailTip;
            }
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
        [SET_SAVED_INLINE_IMAGES]: (state, inlineImages) => {
            state.inlineImagesSaved = inlineImages;
        },
        [SET_MAX_MESSAGE_SIZE](state, size) {
            state.maxMessageSize = size;
        },
        [SHOW_SENDER]: (state, value) => {
            state.isSenderShown = value;
        },
        [UNSET_CORPORATE_SIGNATURE]: state => {
            state.corporateSignature = null;
        },
        [SET_MAIL_TIPS]: (state, mailTips) => {
            state.mailTips = mailTips;
        },
        SET_SAVE_ERROR: (state, error) => {
            state.maxMessageSizeExceeded = error?.data?.errorCode === "ENTITY_TOO_LARGE";
        }
    },

    actions: {
        async [LOAD_MAX_MESSAGE_SIZE]({ commit }, userId) {
            const { messageMaxSize } = await inject("MailboxesPersistence").getMailboxConfig(userId);
            // take into account the email base64 encoding : 33% more space
            commit(SET_MAX_MESSAGE_SIZE, messageMaxSize / 1.33);
        }
    },

    getters: {
        [IS_SENDER_SHOWN]: state => userSettings => state.isSenderShown || userSettings.always_show_from === "true"
    },

    state: {
        disclaimer: null,
        corporateSignature: null,
        personalSignature: null,
        editorContent: "",
        collapsedContent: null,
        inlineImagesSaved: [],
        maxMessageSize: 0, // FIXME: it's a cross-composer data, it must be moved in another store
        isSenderShown: false,
        maxMessageSizeExceeded: false,
        showFormattingToolbar: false,
        synced: ["showFormattingToolbar"],
        mailTips: []
    },
    modules: {
        templateChooser
    }
};
